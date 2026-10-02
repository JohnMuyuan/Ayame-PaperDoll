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

package org.ayamemc.ayamepaperdoll.mixin.hud;

import net.minecraft.client.renderer.SubmitNodeCollection;
import org.ayamemc.ayamepaperdoll.hud.NameTagScaleState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SubmitNodeCollection.class)
public class NameTagFeatureRendererMixin {
    @ModifyConstant(
            method = "submitNameTag",
            constant = @Constant(floatValue = 0.025F)
    )
    private float modifyPositiveNameTagScale(float vanillaScale) {
        return NameTagScaleState.adjust(vanillaScale);
    }

    @ModifyConstant(
            method = "submitNameTag",
            constant = @Constant(floatValue = -0.025F)
    )
    private float modifyNegativeNameTagScale(float vanillaScale) {
        return NameTagScaleState.adjust(vanillaScale);
    }

    @ModifyArg(
            method = "submitNameTag",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;nameTag(Lorg/joml/Matrix4f;FFLnet/minecraft/util/FormattedCharSequence;IIILnet/minecraft/client/gui/Font$DisplayMode;)Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Submit;"
            ),
            index = 1
    )
    private float modifyNameTagX(float vanillaX) {
        return NameTagScaleState.adjustTextX(vanillaX);
    }

    @ModifyArg(
            method = "submitNameTag",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;nameTag(Lorg/joml/Matrix4f;FFLnet/minecraft/util/FormattedCharSequence;IIILnet/minecraft/client/gui/Font$DisplayMode;)Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Submit;"
            ),
            index = 2
    )
    private float modifyNameTagY(float vanillaY) {
        return NameTagScaleState.adjustTextYWithOffset(vanillaY);
    }
}
