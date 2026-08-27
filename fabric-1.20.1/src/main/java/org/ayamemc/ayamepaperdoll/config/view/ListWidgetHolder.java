/*
 *     Highly configurable PaperDoll mod. Forked from Extra Player Renderer.
 *     Copyright (C) 2024-2025  LucunJi(Original author), HappyRespawnanchor
 *
 *     This file is part of Ayame PaperDoll.
 */

package org.ayamemc.ayamepaperdoll.config.view;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * {@link net.minecraft.client.gui.components.tabs.Tab} only accepts {@link AbstractWidget} children,
 * while a selection list is not one on 1.20.1. This holder bridges the two.
 */
public class ListWidgetHolder extends AbstractWidget {
    private final ListWidget list;

    public ListWidgetHolder(ListWidget list) {
        super(0, 0, 0, 0, Component.empty());
        this.list = list;
    }

    public ListWidget getList() {
        return this.list;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.list.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.list.updateNarration(output);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.list.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.list.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.list.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return this.list.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.list.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.list.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.list.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.list.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.list.setFocused(null);
        }
    }
}
