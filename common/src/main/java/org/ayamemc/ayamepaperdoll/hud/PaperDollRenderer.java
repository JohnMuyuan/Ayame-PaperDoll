/*
 *     Highly configurable PaperDoll mod. Forked from Extra Player Renderer.
 *     Copyright (C) 2024-2025  LucunJi(Original author), HappyRespawnanchor
 *
 *     This file is part of Ayame PaperDoll.
 *
 *     Ayame PaperDoll is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Ayame PaperDoll is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with Ayame PaperDoll.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.ayamemc.ayamepaperdoll.hud;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.ayamemc.ayamepaperdoll.config.Configs;
import org.ayamemc.ayamepaperdoll.config.Configs.RotationMode;
import org.ayamemc.ayamepaperdoll.hud.DataBackup.DataBackupEntry;
import org.ayamemc.ayamepaperdoll.mixininterface.GuiGraphicsExtractorInterface;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.ayamemc.ayamepaperdoll.AyamePaperDoll.CONFIGS;

public class PaperDollRenderer {
    private static final List<DataBackupEntry<LivingEntity, ?>> LIVINGENTITY_BACKUP_ENTRIES = ImmutableList.of(
            new DataBackupEntry<>(LivingEntity::getPose, LivingEntity::setPose),
            // required for player on client side
            new DataBackupEntry<>(Entity::isCrouching, (e, flag) -> {
                if (e instanceof LocalPlayer player) player.crouching = flag;
            }),
            new DataBackupEntry<>(e -> e.swimAmount, (e, pitch) -> e.swimAmount = pitch),
            new DataBackupEntry<>(e -> e.swimAmountO, (e, pitch) -> e.swimAmountO = pitch),
            new DataBackupEntry<>(LivingEntity::isFallFlying, (e, flag) -> e.setSharedFlag(7, flag)),
            new DataBackupEntry<>(LivingEntity::getFallFlyingTicks, (e, ticks) -> e.fallFlyTicks = ticks),

            new DataBackupEntry<>(LivingEntity::getVehicle, (e, vehicle) -> e.vehicle = vehicle),

            new DataBackupEntry<>(e -> e.yBodyRotO, (e, yaw) -> e.yBodyRotO = yaw),
            new DataBackupEntry<>(e -> e.yBodyRot, (e, yaw) -> e.yBodyRot = yaw),
            new DataBackupEntry<>(e -> e.yHeadRotO, (e, yaw) -> e.yHeadRotO = yaw),
            new DataBackupEntry<>(e -> e.yHeadRot, (e, yaw) -> e.yHeadRot = yaw),
            new DataBackupEntry<>(e -> e.xRotO, (e, pitch) -> e.xRotO = pitch),
            new DataBackupEntry<>(LivingEntity::getXRot, LivingEntity::setXRot),

            new DataBackupEntry<>(e -> e.attackAnim, (e, prog) -> e.attackAnim = prog),
            new DataBackupEntry<>(e -> e.oAttackAnim, (e, prog) -> e.oAttackAnim = prog),
            new DataBackupEntry<>(e -> e.hurtTime, (e, time) -> e.hurtTime = time),
            new DataBackupEntry<>(LivingEntity::getRemainingFireTicks, LivingEntity::setRemainingFireTicks),
            new DataBackupEntry<>(e -> e.getSharedFlag(0), (e, flag) -> e.setSharedFlag(0, flag)) // on fire
    );
    private static final PaperDollRenderer instance = new PaperDollRenderer();
    private final Minecraft minecraft = Minecraft.getInstance();
    private List<ModRenderState> cachedRenderStates = List.of();
    private RenderCacheKey cachedRenderKey;
    private long lastRenderNanos;
    private int cachedTargetId = Integer.MIN_VALUE;

    private PaperDollRenderer() {
    }

    public static PaperDollRenderer getInstance() {
        return instance;
    }

    @SuppressWarnings("resource")
    private static int getLight(Entity entity, float tickDelta) {
        if (CONFIGS.useWorldLight.getValue()) {
            Level world = entity.level();
            BlockPos lightPos = BlockPos.containing(entity.getEyePosition(tickDelta));
            int blockLight = world.getBrightness(LightLayer.BLOCK, lightPos);
            int skyLight = world.getBrightness(LightLayer.SKY, lightPos);
            int min = CONFIGS.worldLightMin.getValue();
            if (entity.isOnFire()) {
                blockLight = 15;
            }
            blockLight = Mth.clamp(blockLight, min, 15);
            skyLight = Mth.clamp(skyLight, min, 15);
            return LightCoordsUtil.pack(blockLight, skyLight);
        }
        return LightCoordsUtil.pack(15, 15);
    }

    private static float getFallFlyingLeaning(LivingEntity entity, float partialTicks) {
        float ticks = partialTicks + entity.getFallFlyingTicks();
        return Mth.clamp(ticks * ticks / 100f, 0f, 1f);
    }

    // Do not translate the GUI pose here; it breaks Loom rendering.
    // guiGraphics.pose().translate(0, 0, 200);

    /**
     * Mimics the code in {@link InventoryScreen#extractEntityInInventoryFollowsMouse}
     */
    public void extractPaperdoll(GuiGraphicsExtractor graphics, float a) {
        if (minecraft.level == null || minecraft.player == null || !CONFIGS.displayPaperDoll.getValue()) return;
        String playerName = CONFIGS.playerName.getValue();
        LivingEntity targetEntity = playerName.isBlank()
                ? minecraft.player
                : minecraft.level.players().stream().filter(p -> p.getName().getString().equals(playerName)).findFirst().orElse(minecraft.player);
        if (CONFIGS.spectatorAutoSwitch.getValue() && minecraft.player.isSpectator()) {
            Entity cameraEntity = minecraft.getCameraEntity();
            if (cameraEntity instanceof LivingEntity livingEntity) {
                targetEntity = livingEntity;
            } else if (cameraEntity != null) {
                return;
            }
        }

        RenderCacheKey renderKey = createRenderCacheKey(targetEntity);
        int maxRefreshRate = CONFIGS.maxRefreshRate.getValue();
        long now = System.nanoTime();
        if (maxRefreshRate > 0
                && targetEntity.getId() == this.cachedTargetId
                && renderKey.equals(this.cachedRenderKey)
                && !this.cachedRenderStates.isEmpty()
                && now - this.lastRenderNanos < 1_000_000_000L / maxRefreshRate) {
            DollRenderFlags.setReuseTexture(true);
            submitCachedStates(graphics);
            return;
        }
        DollRenderFlags.setReuseTexture(false);

        Configs.PoseOffsetMethod poseOffsetMethod = CONFIGS.poseOffsetMethod.getValue();
        var backup = new DataBackup<>(targetEntity, LIVINGENTITY_BACKUP_ENTRIES);
        backup.save();

        AvatarMotionSnapshot avatarMotion = AvatarMotionSnapshot.capture(targetEntity, a);
        transformEntity(targetEntity, a, poseOffsetMethod == Configs.PoseOffsetMethod.FORCE_STANDING);

        EntityRenderState targetRenderState = extractRenderState(targetEntity, a);
        avatarMotion.apply(targetRenderState);
        applyHalfSyncFacingLimit(targetRenderState);

        this.cachedRenderStates = createPaperdollStates(
                graphics,
                targetRenderState,
                new Vector3f(0, (float) getPoseOffsetY(targetEntity, a, poseOffsetMethod), 0)
        );
        this.cachedRenderKey = renderKey;
        this.cachedTargetId = targetEntity.getId();
        this.lastRenderNanos = now;
        DollParticles.getInstance().update(targetEntity, a);

        backup.restore();
    }

    private double getPoseOffsetY(LivingEntity targetEntity, float partialTicks, Configs.PoseOffsetMethod poseOffsetMethod) {
        if (poseOffsetMethod == Configs.PoseOffsetMethod.AUTO) {
            final float defaultPlayerEyeHeight = Player.DEFAULT_EYE_HEIGHT;
            final float defaultPlayerSwimmingBBHeight = Player.SWIMMING_BB_HEIGHT;
            final float eyeHeightRatio = 0.85f;
            if (targetEntity.isFallFlying()) {
                return (defaultPlayerEyeHeight - targetEntity.getEyeHeight()) * getFallFlyingLeaning(targetEntity, partialTicks);
            } else if (targetEntity.isAutoSpinAttack()) {
                return defaultPlayerEyeHeight - defaultPlayerSwimmingBBHeight * eyeHeightRatio * 0.8;
            } else if (targetEntity.isVisuallySwimming()) {
                return targetEntity.getSwimAmount(partialTicks) <= 0 ? 0 : defaultPlayerEyeHeight - targetEntity.getEyeHeight();
            } else if (!targetEntity.isVisuallySwimming() && targetEntity.getSwimAmount(partialTicks) > 0) { // for swimming/crawling pose, only smooth the falling edge
                return (defaultPlayerEyeHeight - defaultPlayerSwimmingBBHeight * eyeHeightRatio * 0.85) * targetEntity.getSwimAmount(partialTicks);
            } else {
                return Player.DEFAULT_EYE_HEIGHT - targetEntity.getEyeHeight();
            }
        } else if (poseOffsetMethod == Configs.PoseOffsetMethod.MANUAL) {
            if (targetEntity.isFallFlying()) {
                return CONFIGS.elytraOffsetY.getValue() * getFallFlyingLeaning(targetEntity, partialTicks);
            } else if ((targetEntity.isVisuallySwimming()) && targetEntity.getSwimAmount(partialTicks) > 0 || targetEntity.isAutoSpinAttack()) { // require nonzero leaning to filter out glitch
                return CONFIGS.swimCrawlOffsetY.getValue();
            } else if (!targetEntity.isVisuallySwimming() && targetEntity.getSwimAmount(partialTicks) > 0) { // for swimming/crawling pose, only smooth the falling edge
                return CONFIGS.swimCrawlOffsetY.getValue() * targetEntity.getSwimAmount(partialTicks);
            } else if (targetEntity.isCrouching()) {
                return CONFIGS.sneakOffsetY.getValue();
            }
        }
        return 0;
    }

    private void transformEntity(LivingEntity targetEntity, float partialTicks, boolean forceStanding) {
        // synchronize values to remove glitch
        if (!targetEntity.isSwimming() && !targetEntity.isFallFlying() && !targetEntity.isVisuallyCrawling()) {
            targetEntity.setPose(targetEntity.isCrouching() ? Pose.CROUCHING : Pose.STANDING);
        }

        if (forceStanding) {
            if (targetEntity instanceof LocalPlayer player) {
                player.crouching = false;
            }
            targetEntity.vehicle = null;

            targetEntity.swimAmount = 0;
            targetEntity.swimAmountO = 0;

            targetEntity.setSharedFlag(7, false);
            targetEntity.fallFlyTicks = 0;
        }

        // FIXME: NEVERFIX - glitch when the mouse moves too fast, caused by lerping a warped value, it is possibly wrapped in LivingEntity#tick or LivingEntity#turnHead
        final float headLerp = Mth.lerp(partialTicks, targetEntity.yHeadRotO, targetEntity.yHeadRot);
        final float bodyLerp = Mth.lerp(partialTicks, targetEntity.yBodyRotO, targetEntity.yBodyRot);
        final float diff = Mth.wrapDegrees(headLerp - bodyLerp);
        final RotationMode rotationMode = CONFIGS.rotationMode.getValue();
        final boolean syncPlayerMotion = rotationMode == RotationMode.FULL_SYNC || rotationMode == RotationMode.HALF_SYNC;
        final float headClamp;
        final float bodyClamp;
        final float pitchClamp;
        if (syncPlayerMotion) {
            headClamp = headLerp;
            bodyClamp = bodyLerp;
            pitchClamp = Mth.lerp(partialTicks, targetEntity.xRotO, targetEntity.getXRot());
        } else {
            final double headYaw = CONFIGS.headYaw.getValue(), headYawRange = CONFIGS.headYawRange.getValue();
            final double bodyYaw = CONFIGS.bodyYaw.getValue(), bodyYawRange = CONFIGS.bodyYawRange.getValue();
            final double pitch = CONFIGS.pitch.getValue(), pitchRange = CONFIGS.pitchRange.getValue();
            headClamp = (float) Mth.clamp(headLerp, headYaw - headYawRange, headYaw + headYawRange);
            bodyClamp = (float) Mth.clamp(Mth.wrapDegrees(headClamp - diff), bodyYaw - bodyYawRange, bodyYaw + bodyYawRange);
            pitchClamp = (float) (Mth.clamp(Mth.lerp(partialTicks, targetEntity.xRotO, targetEntity.getXRot()), -pitchRange, pitchRange) + pitch);
        }

        // 婵犮垼鍩栭幐鎶藉磿閹绢喗鐓ュù锝呮憸閺?(skip if riding)
        if (rotationMode == RotationMode.LOCK) {
            targetEntity.yHeadRot = targetEntity.yHeadRotO = 180 - headClamp;
        }
        // 闂傚鍓﹂崑鍌滅礊鐎ｎ喗鐓ュù锝呮憸閺?(skip if riding)
        if (rotationMode == RotationMode.LOCK) {
            targetEntity.yBodyRot = targetEntity.yBodyRotO = 180 - bodyClamp;
        }

        // 婵犮垼鍩栭幐鎶藉磿鐎涙鈹嶆い鏍ㄧ箥濞兼帡鎮峰▎鎰瑨閻?        targetEntity.setXRot(targetEntity.xRotO = pitchClamp);


        if (!CONFIGS.swingHands.getValue()) {
            targetEntity.attackAnim = 0;
            targetEntity.oAttackAnim = 0;
        }

        if (!CONFIGS.hurtFlash.getValue()) {
            targetEntity.hurtTime = 0;
        }

    }

    private List<ModRenderState> createPaperdollStates(GuiGraphicsExtractor graphics, EntityRenderState target, Vector3f offset) {
        var scaledWidth = minecraft.getWindow().getGuiScaledWidth();
        var scaledHeight = minecraft.getWindow().getGuiScaledHeight();

        var posX = CONFIGS.offsetX.getValue() * scaledWidth;
        var posY = CONFIGS.offsetY.getValue() * scaledHeight;
        var size = CONFIGS.size.getValue() * scaledHeight;
        var lightDegree = CONFIGS.lightDegree.getValue();

        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.PI);
        Quaternionf configRot = getDisplayRotation();
        pose.mul(configRot).rotateY((float) Math.toRadians(lightDegree + 180));

        boolean splitReadableNameTag = CONFIGS.displayNameTag.getValue()
                && CONFIGS.mirrored.getValue()
                && !CONFIGS.nameTagMirrored.getValue()
                && target.nameTag != null;
        TextureBounds textureBounds = getTextureBounds(target, splitReadableNameTag, (int) posX, (int) posY, (float) size);

        GuiGraphicsExtractorInterface extractor = (GuiGraphicsExtractorInterface) graphics;
        List<ModRenderState> states = new ArrayList<>(splitReadableNameTag ? 2 : 1);
        states.add(new ModRenderState(
                        target,
                        offset,
                        pose,
                        null,
                        null,
                        List.of(),
                        pose,
                        new Quaternionf(configRot).conjugate(),
                        CONFIGS.mirrored.getValue(),
                        splitReadableNameTag,
                        false,
                        (int) posX, (int) posY,
                        textureBounds.x0(), textureBounds.y0(), textureBounds.x1(), textureBounds.y1(),
                        (float) size, null
                ));
        if (splitReadableNameTag) {
            states.add(new ModRenderState(
                            target,
                            offset,
                            pose,
                            null,
                            null,
                            List.of(),
                            pose,
                            new Quaternionf(configRot).conjugate(),
                            false,
                            false,
                            true,
                            (int) posX, (int) posY,
                            textureBounds.x0(), textureBounds.y0(), textureBounds.x1(), textureBounds.y1(),
                            (float) size, null
                    ));
        }
        states.forEach(extractor::addPicturesInPictureState);
        return List.copyOf(states);
    }


    private TextureBounds getTextureBounds(EntityRenderState target, boolean splitReadableNameTag, int x, int y, float scale) {
        float horizontalMultiplier = 3.5F;
        float topMultiplier = 3.75F;
        float bottomMultiplier = 3.5F;
        if (target instanceof AvatarRenderState avatarState
                && (avatarState.fallFlyingTimeInTicks > 0.0F || avatarState.shouldApplyFlyingYRot)) {
            horizontalMultiplier = 7.0F;
            topMultiplier = 12.0F;
            bottomMultiplier = 7.0F;
        }
        if (splitReadableNameTag || target.nameTag != null || target.scoreText != null) {
            topMultiplier += 1.0F;
        }

        int horizontalPadding = Math.max(48, (int) Math.ceil(scale * horizontalMultiplier));
        int topPadding = Math.max(48, (int) Math.ceil(scale * topMultiplier));
        int bottomPadding = Math.max(48, (int) Math.ceil(scale * bottomMultiplier));
        return new TextureBounds(
                x - horizontalPadding,
                y - topPadding,
                x + horizontalPadding,
                y + bottomPadding
        );
    }

    private record TextureBounds(int x0, int y0, int x1, int y1) {
    }

    private void applyHalfSyncFacingLimit(EntityRenderState state) {
        if (CONFIGS.rotationMode.getValue() == RotationMode.HALF_SYNC && state instanceof LivingEntityRenderState livingState) {
            float bodyOffset = Mth.wrapDegrees(livingState.bodyRot - 180.0F);
            livingState.bodyRot = 180.0F + getHalfSyncBodyOffset(bodyOffset);
        }
    }

    private float getHalfSyncBodyOffset(float bodyOffset) {
        return 55.0F * (float) Math.sin(Math.toRadians(bodyOffset));
    }

    //also check =InventoryScreen#extractRenderState
    private EntityRenderState extractRenderState(Entity targetEntity, float a){
        EntityRenderDispatcher entityRenderDispatcher = minecraft.getEntityRenderDispatcher();
        EntityRenderer<? super Entity, ?> entityRenderer = entityRenderDispatcher.getRenderer(targetEntity);
        EntityRenderState state = entityRenderer.createRenderState(targetEntity, a);
        if (!CONFIGS.displayNameTag.getValue()) {
            state.nameTag = null;
            state.scoreText = null;
        } else {
            state.nameTagAttachment = getHeadNameTagAttachment(targetEntity, a);
        }
        state.lightCoords = getLight(targetEntity, a);
        state.shadowPieces.clear();

        // Always disable glow outlines in PIP rendering
        // In PIP context, enabling outlineColor causes the entire entity to render as a white silhouette
        // This is a limitation of PIP rendering - glow effects cannot be properly displayed
        state.outlineColor = EntityRenderState.NO_OUTLINE;

        if (CONFIGS.renderEntityEffects.getValue() && state.isInvisible && state instanceof LivingEntityRenderState livingState) {
            livingState.isInvisibleToPlayer = true;
        }
        if (!CONFIGS.renderEntityEffects.getValue()) {
            // User disabled entity effects - turn off fire and red overlay
            state.displayFireAnimation = false;
            if (state instanceof LivingEntityRenderState livingState) {
                livingState.hasRedOverlay = false;
            }
        }
        // Fire animation is preserved when renderEntityEffects is true
        // Glow effect is always disabled due to PIP rendering limitations
        return state;
    }

    private Quaternionf getDisplayRotation() {
        if (CONFIGS.rotationMode.getValue() == RotationMode.LOCK) {
            return new Quaternionf().rotateXYZ(
                    (float) Math.toRadians(CONFIGS.lockRotationX.getValue()),
                    (float) Math.toRadians(CONFIGS.lockRotationY.getValue()),
                    (float) Math.toRadians(CONFIGS.lockRotationZ.getValue()));
        }
        return new Quaternionf().rotateXYZ(
                (float) Math.toRadians(CONFIGS.rotationX.getValue()),
                (float) Math.toRadians(CONFIGS.rotationY.getValue()),
                (float) Math.toRadians(CONFIGS.rotationZ.getValue()));
    }

    private Vec3 getHeadNameTagAttachment(Entity targetEntity, float partialTicks) {
        Vec3 attachment = targetEntity.getAttachments().getNullable(net.minecraft.world.entity.EntityAttachment.NAME_TAG, 0, targetEntity.getYRot(partialTicks));
        if (attachment == null) {
            attachment = new Vec3(0, targetEntity.getBbHeight(), 0);
        }
        return attachment.add(getNameTagOffset());
    }

    private Vec3 getNameTagOffset() {
        final double textScale = CONFIGS.nameTagSize.getValue() * 0.025D;
        return new Vec3(
                CONFIGS.nameTagOffsetX.getValue() * textScale,
                CONFIGS.nameTagOffsetY.getValue() * textScale,
                0
        );
    }

    private void submitCachedStates(GuiGraphicsExtractor graphics) {
        GuiGraphicsExtractorInterface extractor = (GuiGraphicsExtractorInterface) graphics;
        this.cachedRenderStates.forEach(extractor::addPicturesInPictureState);
    }

    private RenderCacheKey createRenderCacheKey(LivingEntity target) {
        return new RenderCacheKey(
                target.getId(),
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                CONFIGS.offsetX.getValue(),
                CONFIGS.offsetY.getValue(),
                CONFIGS.size.getValue(),
                CONFIGS.mirrored.getValue(),
                CONFIGS.displayNameTag.getValue(),
                CONFIGS.nameTagMirrored.getValue()
        );
    }

    private record RenderCacheKey(int entityId, int width, int height, double offsetX, double offsetY, double size,
                                  boolean mirrored, boolean nameTag, boolean nameTagMirrored) {
    }

    private record AvatarMotionSnapshot(float capeFlap, float capeLean, float capeLean2,
                                        float fallFlyingTime, boolean applyFlyingYRot, float flyingYRot) {
        private static AvatarMotionSnapshot capture(LivingEntity entity, float partialTicks) {
            if (!(entity instanceof ClientAvatarEntity avatar)) return EMPTY;
            ClientAvatarState clientState = avatar.avatarState();
            float fallFlyingTime = entity.getFallFlyingTicks() + partialTicks;
            Vec3 lookAngle = entity.getViewVector(partialTicks);
            Vec3 movement = clientState.deltaMovementOnPreviousTick().lerp(entity.getDeltaMovement(), partialTicks);
            boolean applyFlyingYRot = movement.horizontalDistanceSqr() > 1.0E-5F && lookAngle.horizontalDistanceSqr() > 1.0E-5F;
            float flyingYRot = 0.0F;
            if (applyFlyingYRot) {
                double dot = movement.horizontal().normalize().dot(lookAngle.horizontal().normalize());
                double sign = movement.x * lookAngle.z - movement.z * lookAngle.x;
                flyingYRot = (float) (Math.signum(sign) * Math.acos(Math.min(1.0, Math.abs(dot))));
            }

            double deltaX = clientState.getInterpolatedCloakX(partialTicks) - Mth.lerp(partialTicks, entity.xo, entity.getX());
            double deltaY = clientState.getInterpolatedCloakY(partialTicks) - Mth.lerp(partialTicks, entity.yo, entity.getY());
            double deltaZ = clientState.getInterpolatedCloakZ(partialTicks) - Mth.lerp(partialTicks, entity.zo, entity.getZ());
            float bodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
            double forwardX = Mth.sin(bodyRot * Mth.DEG_TO_RAD);
            double forwardZ = -Mth.cos(bodyRot * Mth.DEG_TO_RAD);
            float capeFlap = Mth.clamp((float) deltaY * 10.0F, -6.0F, 32.0F);
            float flyingScale = Mth.clamp(fallFlyingTime * fallFlyingTime / 100.0F, 0.0F, 1.0F);
            float capeLean = Mth.clamp((float) (deltaX * forwardX + deltaZ * forwardZ) * 100.0F * (1.0F - flyingScale), 0.0F, 150.0F);
            float capeLean2 = Mth.clamp((float) (deltaX * forwardZ - deltaZ * forwardX) * 100.0F, -20.0F, 20.0F);
            capeFlap += Mth.sin(clientState.getInterpolatedWalkDistance(partialTicks) * 6.0F)
                    * 32.0F * clientState.getInterpolatedBob(partialTicks);
            return new AvatarMotionSnapshot(capeFlap, capeLean, capeLean2, fallFlyingTime, applyFlyingYRot, flyingYRot);
        }

        private void apply(EntityRenderState target) {
            if (target instanceof AvatarRenderState avatar) {
                avatar.capeFlap = this.capeFlap;
                avatar.capeLean = this.capeLean;
                avatar.capeLean2 = this.capeLean2;
                avatar.fallFlyingTimeInTicks = this.fallFlyingTime;
                avatar.shouldApplyFlyingYRot = this.applyFlyingYRot;
                avatar.flyingYRot = this.flyingYRot;
            }
        }

        private static final AvatarMotionSnapshot EMPTY = new AvatarMotionSnapshot(0.0F, 0.0F, 0.0F, 0.0F, false, 0.0F);
    }

}
