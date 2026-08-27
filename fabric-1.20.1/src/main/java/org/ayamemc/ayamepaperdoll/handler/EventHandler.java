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

package org.ayamemc.ayamepaperdoll.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.ayamemc.ayamepaperdoll.AyamePaperDoll;
import org.ayamemc.ayamepaperdoll.config.ConfigScreen;
import org.ayamemc.ayamepaperdoll.config.Configs.DisplayPriority;
import org.ayamemc.ayamepaperdoll.config.VisualConfigEditorScreen;
import org.ayamemc.ayamepaperdoll.hud.PaperDollRenderer;
import org.ayamemc.ayamepaperdoll.mixininterface.EntityMixinInterface;

import static org.ayamemc.ayamepaperdoll.AyamePaperDoll.CONFIGS;

public class EventHandler {
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final PaperDollRenderer paperDollRenderer = PaperDollRenderer.getInstance();
    public static Screen lastScreen;

    /**
     * Rendered from the HUD, for both {@link DisplayPriority#DEFAULT} and {@link DisplayPriority#HIGH}.
     * There is only one HUD hook on 1.20.1, so both draw at the same place.
     */
    public static void renderPaperDoll(GuiGraphics graphics, float partialTick) {
        DisplayPriority priority = CONFIGS.displayPriority.getValue();
        if (priority != DisplayPriority.DEFAULT && priority != DisplayPriority.HIGH) return;
        final Player player = minecraft.player;
        if (player == null) return;
        final Pose playerPose = player.getPose();
        if (
                !minecraft.options.hideGui &&
                        !(CONFIGS.hideUnderDebug.getValue() && minecraft.options.renderDebug) &&
                        (minecraft.screen == null || !CONFIGS.hideOnScreenOpen.getValue()) &&
                        !(minecraft.screen instanceof ConfigScreen) &&
                        !(minecraft.screen instanceof VisualConfigEditorScreen) &&
                        (!(CONFIGS.visibleDuringActivity.getValue()) ||
                                (CONFIGS.visibleDuringActivity.getValue() && hasActivity(player, playerPose)))
        ) {
            paperDollRenderer.renderPaperDoll(graphics, partialTick);
        }
    }

    /**
     * Rendered on top of any open screen.
     */
    public static void renderHighestPriorityPaperDoll(GuiGraphics graphics, float partialTick) {
        if (CONFIGS.displayPriority.getValue() != DisplayPriority.HIGHEST) return;
        if (minecraft.screen instanceof PauseScreen) return;
        final Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) return;
        final Pose playerPose = player.getPose();
        if (
                !(minecraft.screen instanceof ConfigScreen) &&
                        !(minecraft.screen instanceof VisualConfigEditorScreen) &&
                        (!(CONFIGS.visibleDuringActivity.getValue()) ||
                                (CONFIGS.visibleDuringActivity.getValue() && hasActivity(player, playerPose)))
        ) {
            paperDollRenderer.renderPaperDoll(graphics, partialTick);
        }
    }

    /**
     * The HUD hook fires for open screens too, so {@link DisplayPriority#HIGHEST} is drawn there when no screen is open.
     */
    public static void renderHighestPriorityPaperDollInHud(GuiGraphics graphics, float partialTick) {
        if (minecraft.screen != null) return;
        if (CONFIGS.displayPriority.getValue() != DisplayPriority.HIGHEST) return;
        final Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) return;
        if (CONFIGS.hideUnderDebug.getValue() && minecraft.options.renderDebug) return;
        final Pose playerPose = player.getPose();
        if (!(CONFIGS.visibleDuringActivity.getValue()) || hasActivity(player, playerPose)) {
            paperDollRenderer.renderPaperDoll(graphics, partialTick);
        }
    }

    private static long lastInactiveTime = 0;
    private static boolean previousState = false;

    public static boolean hasActivity(Player player, Pose playerPose) {
        boolean currentState = playerPose == Pose.SWIMMING ||
                playerPose == Pose.CROUCHING ||
                player.getAbilities().flying ||
                player.isSprinting() ||
                playerPose == Pose.FALL_FLYING ||
                ((EntityMixinInterface) player).ayame_paperdoll$isSitting();

        if (currentState) {
            // 状态为 true 时立即返回 true，并重置时间戳
            previousState = true;
            lastInactiveTime = 0;
        } else {
            // 如果状态从 true 变为 false，记录当前时间戳
            if (previousState && lastInactiveTime == 0) {
                lastInactiveTime = System.currentTimeMillis();
            }

            // 检查是否超过延迟
            if (lastInactiveTime > 0 && System.currentTimeMillis() - lastInactiveTime >= 500) {
                previousState = false;
                lastInactiveTime = 0; // 重置时间戳
            }
        }

        return previousState;
    }

    public static void keyPressed() {
        while (AyamePaperDoll.SHOW_PAPERDOLL_KEY.consumeClick()) {
            CONFIGS.displayPaperDoll.setValue(!CONFIGS.displayPaperDoll.getValue());
        }
        while (AyamePaperDoll.OPEN_CONFIG_GUI.consumeClick()) {
            minecraft.setScreen(new ConfigScreen(lastScreen, AyamePaperDoll.CONFIGS.getOptions()));
        }
    }
}
