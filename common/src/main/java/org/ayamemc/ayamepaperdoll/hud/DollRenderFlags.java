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
 */

package org.ayamemc.ayamepaperdoll.hud;

public final class DollRenderFlags {
    private static boolean reuseTexture;

    private DollRenderFlags() {
    }

    public static boolean shouldReuseTexture() {
        return reuseTexture;
    }

    public static void setReuseTexture(boolean reuseTexture) {
        DollRenderFlags.reuseTexture = reuseTexture;
    }
}
