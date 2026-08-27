/*
 *     Highly configurable PaperDoll mod. Forked from Extra Player Renderer.
 *     Copyright (C) 2024-2025  LucunJi(Original author), HappyRespawnanchor
 *
 *     This file is part of Ayame PaperDoll.
 */

package org.ayamemc.ayamepaperdoll.config.view;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

public class ListWidget extends ContainerObjectSelectionList<ListWidget.ListEntry> {

    private int rowWidth;

    public ListWidget(int width, int screenHeight, int top, int bottom, int entryHeight) {
        super(Minecraft.getInstance(), width, screenHeight, top, bottom, entryHeight);
        this.rowWidth = super.getRowWidth();
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
    }

    @Override
    public int addEntry(ListEntry entry) {
        return super.addEntry(entry);
    }

    @Override
    public int getRowWidth() {
        return this.rowWidth;
    }

    public void setRowWidth(int rowWidth) {
        this.rowWidth = rowWidth;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRowLeft() + this.getRowWidth() + 10;
    }

    public static abstract class ListEntry extends ContainerObjectSelectionList.Entry<ListEntry> {
        private int contentX;
        private int contentY;
        private int contentWidth;

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.contentX = left;
            this.contentY = top;
            this.contentWidth = width;
            this.renderContent(graphics, mouseX, mouseY, hovered, partialTick);
        }

        public abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick);

        protected int getContentX() {
            return this.contentX;
        }

        protected int getContentY() {
            return this.contentY;
        }

        protected int getContentWidth() {
            return this.contentWidth;
        }
    }
}
