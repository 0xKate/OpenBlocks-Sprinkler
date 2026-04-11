package com.zeroxstudios.openblocks_sprinkler;

import com.mojang.logging.LogUtils;
import com.zeroxstudios.openblocks_sprinkler.block.ModBlocks;
import com.zeroxstudios.openblocks_sprinkler.block.ModItems;
import com.zeroxstudios.openblocks_sprinkler.fx.FXLiquidSprayProvider;
import com.zeroxstudios.openblocks_sprinkler.fx.ModParticles;
import com.zeroxstudios.openblocks_sprinkler.ui.ScreenSprinkler;
import com.zeroxstudios.openblocks_sprinkler.network.ModNetwork;
import com.zeroxstudios.openblocks_sprinkler.ui.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.resource.ResourcePackLoader;
import org.slf4j.Logger;

@Mod(Initialization.MODID)
public class Initialization {

    public static final String MODID = "openblocks_sprinkler";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Initialization(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        ModParticles.PARTICLES.register(modBus);

        modBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        var jarFile = context.getContainer().getModInfo().getOwningFile();

        ResourcePackLoader.createPackForMod(jarFile);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.SPRINKLER.get(), ScreenSprinkler::new)
            );
        }

        // Correct 1.19.2 way to register particle providers — fires after
        // particle textures are stitched, so no "redundant texture list" conflict.
        @SubscribeEvent
        public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
            event.register(ModParticles.LIQUID_SPRAY.get(), FXLiquidSprayProvider::new);
        }
    }
}
