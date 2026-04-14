package com.zeroxstudios.openblocks_sprinkler.mixins;

import com.zeroxstudios.openblocks_sprinkler.block.HydrationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public class FarmBlockMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void sprinkler$preventDrying(BlockState state,
                                         ServerLevel level,
                                         BlockPos pos,
                                         RandomSource random,
                                         CallbackInfo ci) {

        if (HydrationManager.isWatered(level, pos)) {
            ci.cancel();
        }
    }
}