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

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import org.ayamemc.ayamepaperdoll.AyamePaperDoll;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.OptionalDouble;

import static org.ayamemc.ayamepaperdoll.AyamePaperDoll.CONFIGS;

public class ModRenderer extends PictureInPictureRenderer<ModRenderState> {
    private final ProjectionMatrixBuffer projectionMatrixBuffer = new ProjectionMatrixBuffer("PIP - " + this.getClass().getSimpleName());
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final Projection projection = new Projection();
    private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();

    private int width, height;

    public ModRenderer(EntityRenderDispatcher entityRenderDispatcher) {
        this.entityRenderDispatcher = entityRenderDispatcher;
    }

    @Override
    public @NotNull Class<ModRenderState> getRenderStateClass() {
        return ModRenderState.class;
    }

    @Override
    protected void renderToTexture(ModRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        // Note: ENTITY_IN_UI lighting mode suppresses fire rendering (by design in vanilla)
        // We still use it to maintain consistent entity appearance with inventory screens
        // Fire effects may not render in this mode - this is a Minecraft limitation
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        Quaternionf quaternionf = renderState.overrideCameraAngle();
        CameraRenderState camerarenderstate = new CameraRenderState();

        if (quaternionf != null) {
            camerarenderstate.orientation = quaternionf.conjugate(new Quaternionf()).rotateY((float) Math.PI);
        }
        NameTagScaleState.setScale(CONFIGS.nameTagSize.getValue().floatValue());
        try {
            if(!renderState.nameTagsOnly() && renderState.vehicleRenderState() != null){
                poseStack.pushPose();
                Vector3f vector3f = renderState.translation2();
                assert vector3f != null;
                poseStack.rotate(renderState.rotation2());
                this.entityRenderDispatcher.submit(renderState.vehicleRenderState(), camerarenderstate, vector3f.x, vector3f.y, vector3f.z, poseStack, submitNodeCollector);
                poseStack.popPose();
            }
            if (!renderState.nameTagsOnly()) for (ModRenderState.ExtraEntityRenderState extraRenderState : renderState.extraRenderStates()) {
                poseStack.pushPose();
                Vector3f vector3f = extraRenderState.translation();
                // Use passenger's own rotation if provided, otherwise use vehicle's rotation
                poseStack.rotate(extraRenderState.rotation() != null ? extraRenderState.rotation() : renderState.rotation2());
                this.entityRenderDispatcher.submit(extraRenderState.renderState(), camerarenderstate, vector3f.x, vector3f.y, vector3f.z, poseStack, submitNodeCollector);
                poseStack.popPose();
            }
            Vector3f vector3f = renderState.translation();
            poseStack.rotate(renderState.rotation());
            if (!renderState.nameTagsOnly()) {
                DollParticles.getInstance().submit(submitNodeCollector, poseStack);
            }
            Component nameTag = renderState.renderState().nameTag;
            Component scoreText = renderState.renderState().scoreText;
            boolean isInvisible = renderState.renderState().isInvisible;
            boolean displayFireAnimation = renderState.renderState().displayFireAnimation;
            boolean isInvisibleToPlayer = renderState.renderState() instanceof LivingEntityRenderState livingState && livingState.isInvisibleToPlayer;
            int outlineColor = renderState.renderState().outlineColor;

            if (renderState.suppressNameTags()) {
                renderState.renderState().nameTag = null;
                renderState.renderState().scoreText = null;
            }
            if (renderState.nameTagsOnly()) {
                renderState.renderState().isInvisible = true;
                renderState.renderState().displayFireAnimation = false;
                renderState.renderState().outlineColor = EntityRenderState.NO_OUTLINE;
                if (renderState.renderState() instanceof LivingEntityRenderState livingState) {
                    livingState.isInvisibleToPlayer = true;
                }
                NameTagScaleState.setNameTagsOnly(true);
            }
            try {
                this.entityRenderDispatcher.submit(renderState.renderState(), camerarenderstate, vector3f.x, vector3f.y, vector3f.z, poseStack, submitNodeCollector);
            } finally {
                renderState.renderState().outlineColor = outlineColor;
                if (renderState.suppressNameTags()) {
                    renderState.renderState().nameTag = nameTag;
                    renderState.renderState().scoreText = scoreText;
                }
                if (renderState.nameTagsOnly()) {
                    renderState.renderState().isInvisible = isInvisible;
                    renderState.renderState().displayFireAnimation = displayFireAnimation;
                    if (renderState.renderState() instanceof LivingEntityRenderState livingState) {
                        livingState.isInvisibleToPlayer = isInvisibleToPlayer;
                    }
                    NameTagScaleState.setNameTagsOnly(false);
                }
            }
        } finally {
            NameTagScaleState.setNameTagsOnly(false);
            NameTagScaleState.clearScale();
        }
    }

    @Override
    protected @NotNull String getTextureLabel() {
        return "ayame-paperdoll";
    }

    @Override
    public void prepare(ModRenderState renderState, GuiRenderState guiRenderState, FeatureRenderDispatcher featureRenderDispatcher, int guiScale) {
        int textureWidth = Math.max(1, renderState.x1() - renderState.x0());
        int textureHeight = Math.max(1, renderState.y1() - renderState.y0());
        int rawTextureWidth = textureWidth * guiScale;
        int rawTextureHeight = textureHeight * guiScale;
        boolean needsAResize = this.width != rawTextureWidth || this.height != rawTextureHeight;
        if (!needsAResize && DollRenderFlags.shouldReuseTexture() && this.textureView != null) {
            blitTexture(renderState, guiRenderState);
            return;
        }
        if (needsAResize) {
            this.width = rawTextureWidth;
            this.height = rawTextureHeight;
        }
        RenderSystem.backupProjectionMatrix();
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        try {
            this.prepareTexturesAndProjection(needsAResize, rawTextureWidth, rawTextureHeight);
            this.projection.setupOrtho(-1000.0F, 1000.0F, textureWidth, textureHeight, true);
            RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(this.projection), ProjectionType.ORTHOGRAPHIC);
            PoseStack posestack = new PoseStack();
            posestack.translate(renderState.originX() - renderState.x0(), renderState.originY() - renderState.y0(), 0.0F);
            float f = renderState.scale();
            posestack.scale(f, f, -f);
            this.renderToTexture(renderState, posestack, this.submitNodeStorage);

            try (FeatureRenderDispatcher.PreparedFrame frame = featureRenderDispatcher.prepareFrame(this.submitNodeStorage);
                 RenderPass renderPass = RenderSystem.getDevice()
                         .createCommandEncoder()
                         .createRenderPass(() -> "Ayame PaperDoll", this.textureView, Optional.empty(), this.depthTextureView, OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(renderPass);
                FeatureRenderDispatcher.renderAllFeatures(renderPass, frame);
            }
        } finally {
            modelViewStack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
        blitTexture(renderState, guiRenderState);
    }

    @Override
    protected void blitTexture(final ModRenderState renderState, final GuiRenderState guiRenderState) {
        float u0,u1;
        if(renderState.textureMirrored()){
            u0 = 1.0f;
            u1 = 0.0f;
        } else {
            u0 = 0.0f;
            u1 = 1.0f;
        }
        guiRenderState.addGlyphToCurrentLayer(
                new ExBlitRenderState(
                        AyamePaperDoll.MOD_PIPELINE,
                        new TextureSetup(
                                this.textureView, null, Minecraft.getInstance().gameRenderer.levelLightmap(), RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST), null, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
                        ),
                        renderState.pose(),
                        renderState.x0(), renderState.y0(), renderState.x1(), renderState.y1(),
                        u0,u1,
                        1.0F,
                        0.0F,
                        renderState.renderState().lightCoords,
                        renderState.scissorArea(),
                        null
                )
        );
    }
}
