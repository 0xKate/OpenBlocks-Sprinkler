package com.zeroxstudios.openblocks_sprinkler;

import com.zeroxstudios.openblocks_sprinkler.block.BlockEntityRendererSprinkler;
import com.zeroxstudios.openblocks_sprinkler.block.ModBlocks;
import com.zeroxstudios.openblocks_sprinkler.block.ModItems;
import com.zeroxstudios.openblocks_sprinkler.fx.FXLiquidSprayProvider;
import com.zeroxstudios.openblocks_sprinkler.fx.ModParticles;
import com.zeroxstudios.openblocks_sprinkler.ui.ScreenSprinkler;
import com.zeroxstudios.openblocks_sprinkler.ui.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.resource.ResourcePackLoader;

@Mod(Initialization.MODID)
public class Initialization {

    public static final String MODID = "openblocks_sprinkler";

    public Initialization(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "openblocks_sprinkler-common.toml");

        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        ModParticles.PARTICLES.register(modBus);

        ResourcePackLoader.createPackForMod(context.getContainer().getModInfo().getOwningFile());
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(ModMenuTypes.SPRINKLER.get(), ScreenSprinkler::new);
                BlockEntityRenderers.register(ModBlocks.SPRINKLER_BE.get(), BlockEntityRendererSprinkler::new);
            });
        }

        @SubscribeEvent
        public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
            event.register(ModParticles.LIQUID_SPRAY.get(), FXLiquidSprayProvider::new);
        }
    }
}
