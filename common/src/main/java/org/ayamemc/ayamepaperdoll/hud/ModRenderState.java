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

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public record ModRenderState(
        EntityRenderState renderState,
        Vector3f translation,
        Quaternionf rotation,
        @Nullable EntityRenderState vehicleRenderState,
        @Nullable Vector3f translation2,
        List<ExtraEntityRenderState> extraRenderStates,
        Quaternionf rotation2,
        @Nullable Quaternionf overrideCameraAngle,
        boolean textureMirrored,
        boolean suppressNameTags,
        boolean nameTagsOnly,
        int originX,
        int originY,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        int texturePadding,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public ModRenderState(
            EntityRenderState renderState,
            Vector3f translation,
            Quaternionf rotation,
            @Nullable EntityRenderState vehicleRenderState,
            @Nullable Vector3f translation2,
            List<ExtraEntityRenderState> extraRenderStates,
            Quaternionf rotation2,
            @Nullable Quaternionf overrideCameraAngle,
            boolean textureMirrored,
            boolean suppressNameTags,
            boolean nameTagsOnly,
            int x,
            int y,
            float scale,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                renderState,
                translation,
                rotation,
                vehicleRenderState,
                translation2,
                extraRenderStates,
                rotation2,
                overrideCameraAngle,
                textureMirrored,
                suppressNameTags,
                nameTagsOnly,
                x,
                y,
                x - getTexturePadding(scale),
                y - getTexturePadding(scale),
                x + getTexturePadding(scale),
                y + getTexturePadding(scale),
                scale,
                getTexturePadding(scale),
                scissorArea,
                PictureInPictureRenderState.getBounds(
                        x - getTexturePadding(scale),
                        y - getTexturePadding(scale),
                        x + getTexturePadding(scale),
                        y + getTexturePadding(scale),
                        scissorArea
                )
        );
    }

    public ModRenderState(
            EntityRenderState renderState,
            Vector3f translation,
            Quaternionf rotation,
            @Nullable EntityRenderState vehicleRenderState,
            @Nullable Vector3f translation2,
            List<ExtraEntityRenderState> extraRenderStates,
            Quaternionf rotation2,
            @Nullable Quaternionf overrideCameraAngle,
            boolean textureMirrored,
            boolean suppressNameTags,
            boolean nameTagsOnly,
            int x,
            int y,
            float scale,
            int texturePadding,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                renderState,
                translation,
                rotation,
                vehicleRenderState,
                translation2,
                extraRenderStates,
                rotation2,
                overrideCameraAngle,
                textureMirrored,
                suppressNameTags,
                nameTagsOnly,
                x,
                y,
                x - texturePadding,
                y - texturePadding,
                x + texturePadding,
                y + texturePadding,
                scale,
                texturePadding,
                scissorArea,
                PictureInPictureRenderState.getBounds(
                        x - texturePadding,
                        y - texturePadding,
                        x + texturePadding,
                        y + texturePadding,
                        scissorArea
                )
        );
    }
    public ModRenderState(
            EntityRenderState renderState,
            Vector3f translation,
            Quaternionf rotation,
            @Nullable EntityRenderState vehicleRenderState,
            @Nullable Vector3f translation2,
            List<ExtraEntityRenderState> extraRenderStates,
            Quaternionf rotation2,
            @Nullable Quaternionf overrideCameraAngle,
            boolean textureMirrored,
            boolean suppressNameTags,
            boolean nameTagsOnly,
            int x,
            int y,
            int x0,
            int y0,
            int x1,
            int y1,
            float scale,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                renderState,
                translation,
                rotation,
                vehicleRenderState,
                translation2,
                extraRenderStates,
                rotation2,
                overrideCameraAngle,
                textureMirrored,
                suppressNameTags,
                nameTagsOnly,
                x,
                y,
                x0,
                y0,
                x1,
                y1,
                scale,
                Math.max(Math.max(x - x0, x1 - x), Math.max(y - y0, y1 - y)),
                scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea)
        );
    }
    private static int getTexturePadding(float scale) {
        return Math.max(16, (int) Math.ceil(scale * 2.5F));
    }

    public record ExtraEntityRenderState(
            EntityRenderState renderState,
            Vector3f translation,
            @Nullable Quaternionf rotation
    ) {
    }
}
