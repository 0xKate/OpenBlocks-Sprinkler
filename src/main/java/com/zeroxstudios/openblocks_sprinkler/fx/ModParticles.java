package com.zeroxstudios.openblocks_sprinkler.fx;

import com.mojang.serialization.Codec;
import com.zeroxstudios.openblocks_sprinkler.Initialization;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Initialization.MODID);

    public static final RegistryObject<ParticleType<FXLiquidSprayData>> LIQUID_SPRAY =
            PARTICLES.register("liquid_spray",
                    () -> new ParticleType<>(false, FXLiquidSprayData.DESERIALIZER) {
                        @Override
                        public @NotNull Codec<FXLiquidSprayData> codec() {
                            return Codec.unit(new FXLiquidSprayData(0x3F76E4, 0.3f, 0.7f));
                        }
                    });
}