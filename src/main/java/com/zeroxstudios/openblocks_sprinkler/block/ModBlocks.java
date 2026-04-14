package com.zeroxstudios.openblocks_sprinkler.block;

import com.zeroxstudios.openblocks_sprinkler.Initialization;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Initialization.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Initialization.MODID);

    public static final RegistryObject<BlockSprinkler> SPRINKLER =
            BLOCKS.register("sprinkler", BlockSprinkler::new);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BlockEntitySprinkler>> SPRINKLER_BE =
            BLOCK_ENTITIES.register("sprinkler_block_entity", () ->
                    BlockEntityType.Builder.of(
                            BlockEntitySprinkler::new,
                            SPRINKLER.get()
                    ).build(null)
            );
}
