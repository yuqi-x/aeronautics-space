package com.yuqi.aeronauticsspace.net;

import com.yuqi.aeronauticsspace.AeronauticsSpace;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 模组网络通道注册。 */
@EventBusSubscriber(modid = AeronauticsSpace.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // 服务端 -> 客户端
        registrar.playToClient(
                OpenPlanetSelectorPayload.TYPE,
                OpenPlanetSelectorPayload.STREAM_CODEC,
                ClientPayloadHandler::handleOpenSelector);

        // 客户端 -> 服务端
        registrar.playToServer(
                TravelToPlanetPayload.TYPE,
                TravelToPlanetPayload.STREAM_CODEC,
                ServerPayloadHandler::handleTravel);
    }

    /** 服务端让某玩家打开星球选择界面 */
    public static void sendOpenPlanetSelector(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenPlanetSelectorPayload());
    }

    /** 客户端发出传送请求 */
    public static void sendTravelRequest(String planetId) {
        PacketDistributor.sendToServer(new TravelToPlanetPayload(planetId));
    }
}
