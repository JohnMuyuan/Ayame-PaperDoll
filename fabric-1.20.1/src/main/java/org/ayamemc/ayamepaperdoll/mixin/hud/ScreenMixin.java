/*
 *     Highly configurable PaperDoll mod. Forked from Extra Player Renderer.
 *     Copyright (C) 2024-2025  LucunJi(Original author), HappyRespawnanchor
 *
 *     This file is part of Ayame PaperDoll.
 */

package org.ayamemc.ayamepaperdoll.mixin.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.ayamemc.ayamepaperdoll.handler.EventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void renderHighestPriorityPaperDoll(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        EventHandler.renderHighestPriorityPaperDoll(graphics, partialTick);
    }
}
