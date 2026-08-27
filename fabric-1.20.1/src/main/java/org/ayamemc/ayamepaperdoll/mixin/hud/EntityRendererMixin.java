/*
 *     Highly configurable PaperDoll mod. Forked from Extra Player Renderer.
 *     Copyright (C) 2024-2025  LucunJi(Original author), HappyRespawnanchor
 *
 *     This file is part of Ayame PaperDoll.
 */

package org.ayamemc.ayamepaperdoll.mixin.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.ayamemc.ayamepaperdoll.hud.NameTagState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    /**
     * The paper doll draws its own name tag, so the vanilla one is skipped while the doll is being rendered.
     */
    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void suppressNameTagOnPaperDoll(Entity entity, Component component, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (NameTagState.isSuppressed()) {
            ci.cancel();
        }
    }
}
