package com.zeroxstudios.openblocks_sprinkler.fx;

import com.mojang.brigadier.StringReader;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record FXLiquidSprayData(String fluidId, float scale, float gravity) implements ParticleOptions {

    public static final Deserializer<FXLiquidSprayData> DESERIALIZER = new Deserializer<>() {
        @Override
        public @NotNull FXLiquidSprayData fromCommand(@NotNull ParticleType<FXLiquidSprayData> type, @NotNull StringReader reader) {
            // minimal support (not used much)
            return new FXLiquidSprayData("minecraft:water", 0.3f, 0.7f);
        }

        @Override
        public @NotNull FXLiquidSprayData fromNetwork(@NotNull ParticleType<FXLiquidSprayData> type, FriendlyByteBuf buf) {
            String id = buf.readUtf();
            float scale = buf.readFloat();
            float gravity = buf.readFloat();
            return new FXLiquidSprayData(id, scale, gravity);
        }
    };

    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticles.LIQUID_SPRAY.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeUtf(fluidId);
        buf.writeFloat(scale);
        buf.writeFloat(gravity);
    }

    @Override
    public @NotNull String writeToString() {
        return fluidId + " " + scale + " " + gravity;
    }
}