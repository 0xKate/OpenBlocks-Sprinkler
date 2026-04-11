package com.zeroxstudios.openblocks_sprinkler.block;

import com.zeroxstudios.openblocks_sprinkler.Initialization;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Initialization.MODID);

    public static final RegistryObject<BlockItem> SPRINKLER =
            ITEMS.register("sprinkler", () ->
                    new BlockItem(ModBlocks.SPRINKLER.get(),
                            new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
}
