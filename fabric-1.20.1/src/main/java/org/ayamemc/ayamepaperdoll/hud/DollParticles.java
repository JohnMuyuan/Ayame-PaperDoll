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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.ayamemc.ayamepaperdoll.AyamePaperDoll.CONFIGS;

/**
 * The doll is not part of the world, so the vanilla particle engine never draws effect particles for it.
 * This is a small stand-in that simulates and draws them on the doll itself.
 */
public final class DollParticles {
    private static final DollParticles INSTANCE = new DollParticles();
    private static final int MAX_PARTICLES = 256;
    private static final int FULL_LIGHT = LightTexture.pack(15, 15);
    private static final ResourceLocation EFFECT_TEXTURE = new ResourceLocation("effect_0");
    private static final ResourceLocation TOTEM_TEXTURE = new ResourceLocation("glitter_0");

    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private int targetEntityId = Integer.MIN_VALUE;
    private long lastGameTime = Long.MIN_VALUE;
    private int totemTicks;

    private DollParticles() {
    }

    public static DollParticles getInstance() {
        return INSTANCE;
    }

    public void update(LivingEntity entity, float partialTick) {
        if (!CONFIGS.renderEffectParticles.getValue()) {
            clear();
            return;
        }
        if (entity.getId() != this.targetEntityId) {
            clear();
            this.targetEntityId = entity.getId();
        }
        long gameTime = entity.level().getGameTime();
        if (this.lastGameTime == Long.MIN_VALUE) this.lastGameTime = gameTime;
        int ticks = (int) Mth.clamp(gameTime - this.lastGameTime, 0L, 4L);
        for (int i = 0; i < ticks; i++) tick(entity);
        for (Particle particle : this.particles) {
            particle.partialTick = partialTick;
            particle.ageWithPartialTick = particle.age + partialTick;
        }
        this.lastGameTime = gameTime;
    }

    public void triggerTotem(int entityId) {
        if (entityId == this.targetEntityId) this.totemTicks = 30;
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource) {
        if (!CONFIGS.renderEffectParticles.getValue() || this.particles.isEmpty()) return;
        TextureAtlas atlas = Minecraft.getInstance().particleEngine.textureAtlas;
        TextureAtlasSprite effect = atlas.getSprite(EFFECT_TEXTURE);
        TextureAtlasSprite totem = atlas.getSprite(TOTEM_TEXTURE);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_PARTICLES));
        render(poseStack.last(), consumer, effect, totem);
    }

    private void tick(LivingEntity entity) {
        Iterator<Particle> iterator = this.particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.tick();
            if (particle.age >= particle.lifetime) iterator.remove();
        }

        double density = CONFIGS.effectParticleDensity.getValue();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (!effect.isVisible()) continue;
            int color = effect.getEffect().getColor();
            float red = (color >> 16 & 0xFF) / 255.0F;
            float green = (color >> 8 & 0xFF) / 255.0F;
            float blue = (color & 0xFF) / 255.0F;
            double baseChance = 1.0 / ((entity.isInvisible() ? 15.0 : 4.0) * (effect.isAmbient() ? 5.0 : 1.0));
            int guaranteed = (int) Math.floor(baseChance * density);
            double remainder = baseChance * density - guaranteed;
            int count = guaranteed + (this.random.nextDouble() < remainder ? 1 : 0);
            for (int i = 0; i < count; i++) {
                float angle = this.random.nextFloat() * Mth.TWO_PI;
                float radius = entity.getBbWidth() * randomRange(0.62F, 0.85F) + 0.06F;
                addParticle(new Particle(
                        Mth.cos(angle) * radius,
                        randomRange(-0.08F, entity.getBbHeight() + 0.08F),
                        Mth.sin(angle) * radius,
                        Mth.cos(angle) * 0.006F, 0.008F, Mth.sin(angle) * 0.006F,
                        red, green, blue, 1.0F,
                        16 + this.random.nextInt(13), false
                ));
            }
        }

        if (this.totemTicks > 0) {
            this.totemTicks--;
            for (int i = 0; i < 4; i++) {
                boolean cyan = this.random.nextBoolean();
                addParticle(new Particle(
                        randomRange(-entity.getBbWidth() * 0.4F, entity.getBbWidth() * 0.4F),
                        this.random.nextFloat() * entity.getBbHeight(),
                        randomRange(-entity.getBbWidth() * 0.4F, entity.getBbWidth() * 0.4F),
                        randomRange(-0.18F, 0.18F), randomRange(0.015F, 0.18F), randomRange(-0.18F, 0.18F),
                        cyan ? 0.15F : 0.55F, cyan ? 0.85F : 1.0F, cyan ? 0.75F : 0.25F, 1.0F,
                        40 + this.random.nextInt(21), true
                ));
            }
        }
    }

    private void render(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite effect, TextureAtlasSprite totem) {
        Vector3f point = new Vector3f();
        for (Particle particle : this.particles) {
            TextureAtlasSprite sprite = particle.totem ? totem : effect;
            float alpha = particle.alpha * Math.min(1.0F, (particle.lifetime - particle.ageWithPartialTick) / 8.0F);
            float x = Mth.lerp(particle.partialTick, particle.oldX, particle.x);
            float y = Mth.lerp(particle.partialTick, particle.oldY, particle.y);
            float z = Mth.lerp(particle.partialTick, particle.oldZ, particle.z);
            point.set(x, y, z);
            float size = particle.totem ? 0.15F : 0.105F;
            quad(pose, consumer, point, size, sprite, particle.red, particle.green, particle.blue, alpha, false);
            quad(pose, consumer, point, size, sprite, particle.red, particle.green, particle.blue, alpha, true);
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, Vector3f center, float size,
                             TextureAtlasSprite sprite, float red, float green, float blue, float alpha, boolean reverse) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        if (reverse) {
            vertex(pose, consumer, center.x - size, center.y - size, center.z, red, green, blue, alpha, u0, v1);
            vertex(pose, consumer, center.x - size, center.y + size, center.z, red, green, blue, alpha, u0, v0);
            vertex(pose, consumer, center.x + size, center.y + size, center.z, red, green, blue, alpha, u1, v0);
            vertex(pose, consumer, center.x + size, center.y - size, center.z, red, green, blue, alpha, u1, v1);
        } else {
            vertex(pose, consumer, center.x - size, center.y - size, center.z, red, green, blue, alpha, u0, v1);
            vertex(pose, consumer, center.x + size, center.y - size, center.z, red, green, blue, alpha, u1, v1);
            vertex(pose, consumer, center.x + size, center.y + size, center.z, red, green, blue, alpha, u1, v0);
            vertex(pose, consumer, center.x - size, center.y + size, center.z, red, green, blue, alpha, u0, v0);
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z,
                               float red, float green, float blue, float alpha, float u, float v) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_LIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    private void addParticle(Particle particle) {
        if (this.particles.size() >= MAX_PARTICLES) this.particles.remove(0);
        this.particles.add(particle);
    }

    private float randomRange(float min, float max) {
        return Mth.lerp(this.random.nextFloat(), min, max);
    }

    private void clear() {
        this.particles.clear();
        this.totemTicks = 0;
        this.lastGameTime = Long.MIN_VALUE;
    }

    private static final class Particle {
        private float x, y, z;
        private float oldX, oldY, oldZ;
        private float velocityX, velocityY, velocityZ;
        private final float red, green, blue, alpha;
        private final int lifetime;
        private final boolean totem;
        private int age;
        private float partialTick;
        private float ageWithPartialTick;

        private Particle(float x, float y, float z, float velocityX, float velocityY, float velocityZ,
                         float red, float green, float blue, float alpha, int lifetime, boolean totem) {
            this.x = this.oldX = x;
            this.y = this.oldY = y;
            this.z = this.oldZ = z;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            this.lifetime = lifetime;
            this.totem = totem;
        }

        private void tick() {
            this.oldX = this.x;
            this.oldY = this.y;
            this.oldZ = this.z;
            this.x += this.velocityX;
            this.y += this.velocityY;
            this.z += this.velocityZ;
            this.velocityX *= 0.92F;
            this.velocityZ *= 0.92F;
            this.velocityY = this.totem ? (this.velocityY - 0.004F) * 0.96F : this.velocityY * 0.96F;
            this.age++;
        }
    }
}
