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
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.ayamemc.ayamepaperdoll.config.Configs;
import org.ayamemc.ayamepaperdoll.config.Configs.RotationMode;
import org.ayamemc.ayamepaperdoll.hud.DataBackup.DataBackupEntry;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

import static org.ayamemc.ayamepaperdoll.AyamePaperDoll.CONFIGS;

/**
 * Renders the paper doll straight into the HUD, mimicking the code in
 * {@link net.minecraft.client.gui.screens.inventory.InventoryScreen#renderEntityInInventory}.
 */
public class PaperDollRenderer {
    private static final List<DataBackupEntry<LivingEntity, ?>> LIVINGENTITY_BACKUP_ENTRIES = ImmutableList.of(
            new DataBackupEntry<>(LivingEntity::getPose, LivingEntity::setPose),
            // required for player on client side
            new DataBackupEntry<>(Entity::isCrouching, (e, flag) -> {
                if (e instanceof LocalPlayer player) player.crouching = flag;
            }),
            new DataBackupEntry<>(e -> e.swimAmount, (e, amount) -> e.swimAmount = amount),
            new DataBackupEntry<>(e -> e.swimAmountO, (e, amount) -> e.swimAmountO = amount),
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
            return LightTexture.pack(blockLight, skyLight);
        }
        return LightTexture.pack(15, 15);
    }

    private static float getFallFlyingLeaning(LivingEntity entity, float partialTicks) {
        float ticks = partialTicks + entity.getFallFlyingTicks();
        return Mth.clamp(ticks * ticks / 100f, 0f, 1f);
    }

    public void renderPaperDoll(GuiGraphics graphics, float partialTicks) {
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

        Configs.PoseOffsetMethod poseOffsetMethod = CONFIGS.poseOffsetMethod.getValue();
        var backup = new DataBackup<>(targetEntity, LIVINGENTITY_BACKUP_ENTRIES);
        backup.save();
        try {
            transformEntity(targetEntity, partialTicks, poseOffsetMethod == Configs.PoseOffsetMethod.FORCE_STANDING);
            DollParticles.getInstance().update(targetEntity, partialTicks);
            renderDoll(graphics, targetEntity, partialTicks, (float) getPoseOffsetY(targetEntity, partialTicks, poseOffsetMethod));
        } finally {
            backup.restore();
        }
    }

    private void renderDoll(GuiGraphics graphics, LivingEntity entity, float partialTicks, float offsetY3d) {
        int scaledWidth = minecraft.getWindow().getGuiScaledWidth();
        int scaledHeight = minecraft.getWindow().getGuiScaledHeight();

        float posX = (float) (CONFIGS.offsetX.getValue() * scaledWidth);
        float posY = (float) (CONFIGS.offsetY.getValue() * scaledHeight);
        float size = (float) (CONFIGS.size.getValue() * scaledHeight);
        double lightDegree = CONFIGS.lightDegree.getValue();
        boolean mirrored = CONFIGS.mirrored.getValue();
        int light = getLight(entity, partialTicks);

        if (!CONFIGS.renderEntityEffects.getValue()) {
            // the user disabled entity effects: no fire animation
            entity.setRemainingFireTicks(0);
            entity.setSharedFlag(0, false);
        }

        Quaternionf configRot = getDisplayRotation();
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.PI);
        pose.mul(configRot).rotateY((float) Math.toRadians(lightDegree + 180));

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = graphics.bufferSource();
        PoseStack poseStack = graphics.pose();

        Lighting.setupForEntityInInventory();
        dispatcher.overrideCameraOrientation(new Quaternionf(configRot).conjugate());
        dispatcher.setRenderShadow(false);

        poseStack.pushPose();
        poseStack.translate(posX, posY, 50.0F);
        poseStack.mulPoseMatrix(new Matrix4f().scaling(mirrored ? -size : size, size, -size));
        poseStack.mulPose(pose);

        NameTagState.setSuppressed(true);
        try {
            RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0.0, offsetY3d, 0.0, 0.0F, partialTicks, poseStack, bufferSource, light));
            poseStack.pushPose();
            poseStack.translate(0.0F, offsetY3d, 0.0F);
            DollParticles.getInstance().render(poseStack, bufferSource);
            poseStack.popPose();
        } finally {
            NameTagState.setSuppressed(false);
        }
        poseStack.popPose();

        if (CONFIGS.displayNameTag.getValue()) {
            boolean nameTagMirrored = mirrored && CONFIGS.nameTagMirrored.getValue();
            poseStack.pushPose();
            poseStack.translate(posX, posY, 50.0F);
            poseStack.mulPoseMatrix(new Matrix4f().scaling(nameTagMirrored ? -size : size, size, -size));
            poseStack.mulPose(pose);
            poseStack.translate(0.0F, offsetY3d, 0.0F);
            renderNameTag(entity, dispatcher, poseStack, bufferSource, light);
            poseStack.popPose();
        }

        graphics.flush();
        dispatcher.setRenderShadow(true);
        Lighting.setupFor3DItems();
    }

    private void renderNameTag(LivingEntity entity, EntityRenderDispatcher dispatcher, PoseStack poseStack,
                               MultiBufferSource bufferSource, int light) {
        final float scale = CONFIGS.nameTagSize.getValue().floatValue();
        if (scale <= 0.0F) return;
        final float textScale = scale * 0.025F;
        final float anchor = entity.getNameTagOffsetY();

        EntityRenderer<? super LivingEntity> renderer = dispatcher.getRenderer(entity);
        poseStack.pushPose();
        poseStack.translate(
                (float) (CONFIGS.nameTagOffsetX.getValue() * textScale),
                (float) (CONFIGS.nameTagOffsetY.getValue() * textScale),
                0.0F);
        // scale the name tag around its anchor point
        poseStack.translate(0.0F, anchor, 0.0F);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, -anchor, 0.0F);
        renderer.renderNameTag(entity, entity.getDisplayName(), poseStack, bufferSource, light);
        poseStack.popPose();
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

        // FIXME: NEVERFIX - glitch when the mouse moves too fast, caused by lerping a warped value
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

        // head and body rotation
        if (rotationMode == RotationMode.LOCK) {
            targetEntity.yHeadRot = targetEntity.yHeadRotO = 180 - headClamp;
            targetEntity.yBodyRot = targetEntity.yBodyRotO = 180 - bodyClamp;
        } else if (rotationMode == RotationMode.HALF_SYNC) {
            // limit how far the body may turn away from the viewer
            final float limitedBody = 180.0F + getHalfSyncBodyOffset(Mth.wrapDegrees(bodyLerp - 180.0F));
            targetEntity.yBodyRot = targetEntity.yBodyRotO = limitedBody;
            targetEntity.yHeadRot = targetEntity.yHeadRotO = limitedBody + diff;
        }

        // pitch
        targetEntity.setXRot(targetEntity.xRotO = pitchClamp);

        if (!CONFIGS.swingHands.getValue()) {
            targetEntity.attackAnim = 0;
            targetEntity.oAttackAnim = 0;
        }

        if (!CONFIGS.hurtFlash.getValue()) {
            targetEntity.hurtTime = 0;
        }
    }

    private float getHalfSyncBodyOffset(float bodyOffset) {
        return 55.0F * (float) Math.sin(Math.toRadians(bodyOffset));
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
}
