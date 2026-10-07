package com.yuqi.aeronauticsspace.net;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 客户端接收服务端指令。 */
public final class ClientPayloadHandler {

    private ClientPayloadHandler() {
    }

    public static void handleOpenSelector(final OpenPlanetSelectorPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new PlanetSelectScreen()));
    }
}
