package com.zeroxstudios.openblocks_sprinkler.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import org.jetbrains.annotations.NotNull;

public class FXLiquidSprayProvider implements ParticleProvider<FXLiquidSprayData> {

    private final SpriteSet sprites;

    public FXLiquidSprayProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Override
    public Particle createParticle(@NotNull FXLiquidSprayData data, @NotNull ClientLevel level,
                                   double x, double y, double z,
                                   double dx, double dy, double dz) {
        return new FXLiquidSpray(level, x, y, z, dx, dy, dz,
                data.scale(), data.gravity(), sprites, data);  // pass data here
    }
}