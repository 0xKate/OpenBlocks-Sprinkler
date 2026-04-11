package com.zeroxstudios.openblocks_sprinkler.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public class FXLiquidSpray extends TextureSheetParticle {

    protected FXLiquidSpray(ClientLevel level, double x, double y, double z,
                            double dx, double dy, double dz,
                            float scale, float gravity, SpriteSet sprites,
                            FXLiquidSprayData data) {
        super(level, x, y, z, dx, dy, dz);

        this.gravity = gravity;
        this.lifetime = 50;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.setSize(0.2f, 0.2f);
        this.quadSize = scale;
        this.hasPhysics = true;
        this.setSpriteFromAge(sprites);

        // resolve fluid color from the fluid registry
        var fluid = net.minecraftforge.registries.ForgeRegistries.FLUIDS
                .getValue(ResourceLocation.parse(data.fluidId()));
        if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
            FluidStack stack = new FluidStack(fluid, 1);
            int color = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions
                    .of(fluid).getTintColor(stack);
            this.rCol = ((color >> 16) & 0xFF) / 255f;
            this.gCol = ((color >> 8)  & 0xFF) / 255f;
            this.bCol = ( color        & 0xFF) / 255f;
        } else {
            // fallback water blue
            this.rCol = 0.2f;
            this.gCol = 0.3f;
            this.bCol = 1.0f;
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = 1.0f - ((float) this.age / (float) this.lifetime);
    }
}