package com.yuqi.aeronauticsspace.net;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 服务端接收客户端请求。 */
public final class ServerPayloadHandler {

    private ServerPayloadHandler() {
    }

    public static void handleTravel(final TravelToPlanetPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PlanetTravel.travel(serverPlayer, payload.planetId());
            }
        });
    }
}
