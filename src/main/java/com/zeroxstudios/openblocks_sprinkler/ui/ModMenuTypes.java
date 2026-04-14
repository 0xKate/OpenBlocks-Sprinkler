package com.zeroxstudios.openblocks_sprinkler.ui;

import com.zeroxstudios.openblocks_sprinkler.Initialization;
import com.zeroxstudios.openblocks_sprinkler.block.BlockEntitySprinkler;
import com.zeroxstudios.openblocks_sprinkler.block.ContainerSprinkler;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Initialization.MODID);

    public static final RegistryObject<MenuType<ContainerSprinkler>> SPRINKLER =
            MENUS.register("sprinkler", () ->
                    IForgeMenuType.create((windowId, inv, data) -> {
                        net.minecraft.core.BlockPos pos = data.readBlockPos();
                        net.minecraft.world.level.Level level = inv.player.getCommandSenderWorld();
                        BlockEntitySprinkler be = (BlockEntitySprinkler) level.getBlockEntity(pos);
                        assert be != null;
                        return new ContainerSprinkler(windowId, inv, be);
                    }));
}
