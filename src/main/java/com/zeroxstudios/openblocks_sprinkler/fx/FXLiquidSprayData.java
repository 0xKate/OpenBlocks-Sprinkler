package com.zeroxstudios.openblocks_sprinkler.fx;

import com.mojang.brigadier.StringReader;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record FXLiquidSprayData(int color, float scale, float gravity) implements ParticleOptions {

    public static final Deserializer<FXLiquidSprayData> DESERIALIZER = new Deserializer<>() {
        @Override
        public @NotNull FXLiquidSprayData fromCommand(@NotNull ParticleType<FXLiquidSprayData> type, @NotNull StringReader reader) {
            return new FXLiquidSprayData(0x3F76E4, 0.3f, 0.7f);
        }

        @Override
        public @NotNull FXLiquidSprayData fromNetwork(@NotNull ParticleType<FXLiquidSprayData> type, FriendlyByteBuf buf) {
            int color = buf.readInt();
            float scale = buf.readFloat();
            float gravity = buf.readFloat();
            return new FXLiquidSprayData(color, scale, gravity);
        }
    };

    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticles.LIQUID_SPRAY.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(color);
        buf.writeFloat(scale);
        buf.writeFloat(gravity);
    }

    @Override
    public @NotNull String writeToString() {
        return color + " " + scale + " " + gravity;
    }
}