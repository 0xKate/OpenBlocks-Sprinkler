package com.zeroxstudios.openblocks_sprinkler.network;

import com.zeroxstudios.openblocks_sprinkler.Initialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Initialization.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        CHANNEL.registerMessage(0, PacketSprayParticle.class,
                PacketSprayParticle::encode,
                PacketSprayParticle::decode,
                PacketSprayParticle::handle
        );
    }

    public static void sendToNearby(PacketSprayParticle packet, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        CHANNEL.send(
                PacketDistributor.NEAR.with(() ->
                        new PacketDistributor.TargetPoint(pos.getX(), pos.getY(), pos.getZ(), 64, level.dimension())
                ),
                packet
        );
    }
}