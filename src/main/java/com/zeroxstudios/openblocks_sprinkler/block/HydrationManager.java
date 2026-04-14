package com.zeroxstudios.openblocks_sprinkler.block;

import com.zeroxstudios.openblocks_sprinkler.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class HydrationManager {

    private static final Map<ServerLevel, Set<BlockPos>> WATERED = new WeakHashMap<>();

    public static boolean isWatered(ServerLevel level, BlockPos pos) {
        return WATERED.getOrDefault(level, Set.of()).contains(pos);
    }

    public static void setWatered(ServerLevel level, BlockPos pos, boolean value) {
        WATERED.computeIfAbsent(level, l -> new HashSet<>());

        if (value) {
            WATERED.get(level).add(pos.immutable());
        } else {
            WATERED.get(level).remove(pos);
        }
    }

    public static void clearAreaForSprinkler(ServerLevel serverLevel, BlockPos worldPosition) {

        Set<BlockPos> set = WATERED.get(serverLevel);
        if (set == null) return;

        int range = Config.EFFECTIVE_RANGE.get(); // assuming EFFECTIVE_RANGE = 4 for a 9x9 area

        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {

                BlockPos target = worldPosition.offset(dx, -1, dz);

                set.remove(target);
            }
        }

        // optional cleanup: remove empty sets to avoid memory buildup
        if (set.isEmpty()) {
            WATERED.remove(serverLevel);
        }
    }
}