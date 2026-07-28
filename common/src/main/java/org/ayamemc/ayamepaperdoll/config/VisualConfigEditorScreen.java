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

package org.ayamemc.ayamepaperdoll.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.ayamemc.ayamepaperdoll.config.Configs.RotationMode;
import org.ayamemc.ayamepaperdoll.config.model.SimpleNumericOption;
import org.ayamemc.ayamepaperdoll.hud.PaperDollRenderer;
import org.lwjgl.glfw.GLFW;

import static org.ayamemc.ayamepaperdoll.AyamePaperDoll.CONFIGS;

public class VisualConfigEditorScreen extends Screen {
    private static final int LINE_COLOR = 0xA6_FFFFFF;
    private static final int BORDER_MARGIN = 10;
    private final Screen lastScreen;
    private final PaperDollRenderer paperDollRenderer = PaperDollRenderer.getInstance();

    protected VisualConfigEditorScreen(Screen lastScreen) {
        super(Component.empty());
        this.lastScreen = lastScreen;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // 十字的水平线，- 1能居中点，大概
        graphics.horizontalLine(0, width, (height / 2) - 1, LINE_COLOR);
        // 十字的垂直线
        graphics.verticalLine((width / 2), -1, height, LINE_COLOR);

        // 底下的线
        graphics.horizontalLine(0, width, (height - BORDER_MARGIN), LINE_COLOR);
        // 顶上的线
        graphics.horizontalLine(0, width, BORDER_MARGIN, LINE_COLOR);
        // 左边的线
        graphics.verticalLine((width - BORDER_MARGIN), -1, height, LINE_COLOR);
        // 右边的线
        graphics.verticalLine(BORDER_MARGIN, -1, height, LINE_COLOR);
        paperDollRenderer.extractPaperdoll(graphics, a);
    }


    @SuppressWarnings("DataFlowIssue")
    @Override
    public void onClose() {
        // 通过构造新配置屏幕刷新设置中的值

        if (lastScreen instanceof ConfigScreen configScreen) {
            configScreen.onClose();
            this.minecraft.setScreen(new ConfigScreen(lastScreen, CONFIGS.getOptions()));
        } else {
            this.minecraft.setScreen(lastScreen);
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        boolean onDrag = false;
        if (mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (hasShiftDown()) {
                final double divisor = Math.max(CONFIGS.size.getValue() * Math.max(CONFIGS.nameTagSize.getValue(), 0.1) * 0.025, 0.001);
                CONFIGS.nameTagOffsetX.setValue(CONFIGS.nameTagOffsetX.getValue() + deltaX / divisor);
                CONFIGS.nameTagOffsetY.setValue(CONFIGS.nameTagOffsetY.getValue() + deltaY / divisor);
            } else {
                final double newOffsetX = CONFIGS.offsetX.getValue() + (deltaX * 0.0015);
                final double newOffsetY = CONFIGS.offsetY.getValue() + (deltaY * 0.0015);
                if (newOffsetX < CONFIGS.offsetX.getMax() && newOffsetY > CONFIGS.offsetY.getMin()) {
                    CONFIGS.offsetX.setValue(newOffsetX);
                }
                if (newOffsetY < CONFIGS.offsetY.getMax() && newOffsetX > CONFIGS.offsetX.getMin()) {
                    CONFIGS.offsetY.setValue(newOffsetY);
                }
            }
            onDrag = true;
        }
        if (mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            SimpleNumericOption<Double> rotationX = getActiveRotationX();
            SimpleNumericOption<Double> rotationY = getActiveRotationY();
            SimpleNumericOption<Double> rotationZ = getActiveRotationZ();
            if (isControlPressed()) {
                setClamped(rotationZ, rotationZ.getValue() + deltaX);
            } else {
                setClamped(rotationY, rotationY.getValue() + deltaX);
                setClamped(rotationX, rotationX.getValue() + deltaY);
            }
            onDrag = true;
        }
        return onDrag;
    }

    private boolean hasShiftDown() {
        long window = this.minecraft.getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private boolean isControlPressed() {
        long window = this.minecraft.getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private SimpleNumericOption<Double> getActiveRotationX() {
        return CONFIGS.rotationMode.getValue() == RotationMode.LOCK ? CONFIGS.lockRotationX : CONFIGS.rotationX;
    }

    private SimpleNumericOption<Double> getActiveRotationY() {
        return CONFIGS.rotationMode.getValue() == RotationMode.LOCK ? CONFIGS.lockRotationY : CONFIGS.rotationY;
    }

    private SimpleNumericOption<Double> getActiveRotationZ() {
        return CONFIGS.rotationMode.getValue() == RotationMode.LOCK ? CONFIGS.lockRotationZ : CONFIGS.rotationZ;
    }

    private void setClamped(SimpleNumericOption<Double> option, double value) {
        option.setValue(Math.max(option.getMin(), Math.min(option.getMax(), value)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            final double newSize = CONFIGS.size.getValue() + (scrollY / 80);
            if (newSize < CONFIGS.size.getMax() && newSize > CONFIGS.size.getMin()) {
                CONFIGS.size.setValue(newSize);
            }
            return true;
        }
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

}
