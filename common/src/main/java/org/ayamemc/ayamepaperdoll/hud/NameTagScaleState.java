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

public final class NameTagScaleState {
    private static final ThreadLocal<Float> SCALE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> NAME_TAGS_ONLY = ThreadLocal.withInitial(() -> false);

    private NameTagScaleState() {
    }

    public static void setScale(float scale) {
        SCALE.set(scale);
    }

    public static void clearScale() {
        SCALE.remove();
        NAME_TAGS_ONLY.remove();
    }

    public static float adjust(float vanillaScale) {
        Float scale = SCALE.get();
        return scale == null ? vanillaScale : vanillaScale * scale;
    }

    public static float adjustTextY(float vanillaY) {
        Float scale = SCALE.get();
        if (scale == null || scale == 0.0F) return vanillaY;
        return vanillaY + 9.0F * (1.0F / scale - 1.0F);
    }

    public static float adjustTextX(float vanillaX) {
        return vanillaX;
    }

    public static float adjustTextYWithOffset(float vanillaY) {
        return adjustTextY(vanillaY);
    }

    public static void setNameTagsOnly(boolean nameTagsOnly) {
        NAME_TAGS_ONLY.set(nameTagsOnly);
    }

    public static boolean isNameTagsOnly() {
        return NAME_TAGS_ONLY.get();
    }
}
