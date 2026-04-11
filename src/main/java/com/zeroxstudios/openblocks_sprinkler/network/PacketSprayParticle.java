package com.zeroxstudios.openblocks_sprinkler.network;

import com.zeroxstudios.openblocks_sprinkler.fx.FXLiquidSprayData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSprayParticle {

    private final double x, y, z;
    private final double velX, velY, velZ;

    public PacketSprayParticle(double x, double y, double z,
                               double velX, double velY, double velZ) {
        this.x = x; this.y = y; this.z = z;
        this.velX = velX; this.velY = velY; this.velZ = velZ;
    }

    public static void encode(PacketSprayParticle msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeDouble(msg.velX);
        buf.writeDouble(msg.velY);
        buf.writeDouble(msg.velZ);
    }

    public static PacketSprayParticle decode(FriendlyByteBuf buf) {
        return new PacketSprayParticle(
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble()
        );
    }

    public static void handle(PacketSprayParticle msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientHandler.spawnParticle(msg)
                )
        );
        ctx.get().setPacketHandled(true);
    }

    // inner class keeps client-only code off the server classloader
    public static class ClientHandler {
        public static void spawnParticle(PacketSprayParticle msg) {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            var engine = Minecraft.getInstance().particleEngine;
            var particle = engine.createParticle(
                    new FXLiquidSprayData("minecraft:water", 0.2f, 0.8f),
                    msg.x, msg.y, msg.z,
                    msg.velX, msg.velY, msg.velZ
            );
        }
    }
}