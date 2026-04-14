package com.zeroxstudios.openblocks_sprinkler.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zeroxstudios.openblocks_sprinkler.Config;
import com.zeroxstudios.openblocks_sprinkler.fx.FXLiquidSprayData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;

public class BlockEntityRendererSprinkler implements BlockEntityRenderer<BlockEntitySprinkler> {

    public BlockEntityRendererSprinkler(BlockEntityRendererProvider.Context ignoredCtx) {
    }

    private int clientTicks = 0;
    private boolean playerInRange = false;
    private int waterColor = 0x3F76E4;

    @Override
    public void render(BlockEntitySprinkler be, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int light, int overlay)
    {

        Level level = be.getLevel();
        if (level == null) return;

        if (!(level instanceof ClientLevel clientLevel)) return;

        BlockState state = be.getBlockState();
        var enabled = state.getValue(BlockSprinkler.ENABLED);

        if (!enabled) return;

        var pos = be.getBlockPos();

        renderThink(be, state, pos, clientLevel);

    }

    // Spray angle: sine wave -1..1, used by client particle system
    public float getSprayAngle(long gameTime) {
        return (float) Math.sin(gameTime * Config.ANGLE_RICHNESS.get());
    }

    private void renderThink(BlockEntitySprinkler be, BlockState state, BlockPos pos, ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (clientTicks % 20 == 0) {
            if (player == null) {
                playerInRange = false;
            } else {
                double dx = player.getX() - (pos.getX() + 0.5);
                double dy = player.getY() - (pos.getY() + 0.5);
                double dz = player.getZ() - (pos.getZ() + 0.5);
                double maxDistSq = Config.VIEW_DISTANCE.get() * (double) Config.VIEW_DISTANCE.get();
                playerInRange = (dx*dx + dy*dy + dz*dz) <= maxDistSq;
            }

            var stack = be.getTank().getFluid();
            if (!stack.isEmpty())
                waterColor = IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack);
        }

        if (playerInRange)
            if (clientTicks % Config.PARTICLE_TICK_DELAY.get() == 0)
                spawnSprayParticles(level, pos, state, mc);

        clientTicks++;
    }

    // Client Methods
    private void spawnSprayParticles(ClientLevel level, BlockPos pos, BlockState state, Minecraft mc) {
        Direction facing = state.getValue(BlockSprinkler.FACING);

        final double nozzleAngle = getSprayAngle(level.getGameTime());

        double rangeScale = Math.sqrt((double) Config.EFFECTIVE_RANGE.get() / Config.EFFECTIVE_RANGE.getDefault());

        double angleGrowth = 25.0;
        double velocityGrowth = 0.6;

        double sweepAngle = 25.0 + (angleGrowth * (rangeScale - 1.0)); // 25° at default, 39° at 2x range
        double velocityScale = Math.pow(rangeScale, velocityGrowth); // gentler velocity growth

        final double sprayForwardVelocity = Math.sin(Math.toRadians(nozzleAngle * sweepAngle)) * velocityScale;
        final double sprayYVelocity = 0.40f * velocityScale;
        double spraySideScatter = Math.toRadians(sweepAngle);

        final int offsetX = facing.getStepX();
        final int offsetZ = facing.getStepZ();

        final double forwardVelocityX = sprayForwardVelocity * offsetZ / -2.0;
        final double forwardVelocityZ = sprayForwardVelocity * offsetX / 2.0;

        double outletPosition = -0.5;
        while (outletPosition <= 0.5) {
            final double spraySideVelocity = Math.sin(spraySideScatter * (level.random.nextDouble() - 0.5));

            double velX = forwardVelocityX + spraySideVelocity * offsetX;
            double velZ = forwardVelocityZ + spraySideVelocity * offsetZ;

            var x = pos.getX() + 0.5 + (outletPosition * 0.6 * offsetX);
            var y = pos.getY() + 0.10;
            var z = pos.getZ() + 0.5 + (outletPosition * 0.6 * offsetZ);

            var engine = mc.particleEngine;
            engine.createParticle(
                    new FXLiquidSprayData(waterColor, 0.2f, 0.8f),
                    x, y, z,
                    velX, sprayYVelocity, velZ
            );

            outletPosition += 0.2;
        }
    }

}
