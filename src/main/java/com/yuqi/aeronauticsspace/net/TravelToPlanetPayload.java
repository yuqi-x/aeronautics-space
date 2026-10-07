package com.yuqi.aeronauticsspace.net;

import com.yuqi.aeronauticsspace.AeronauticsSpace;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：请求传送到某颗星球。 */
public record TravelToPlanetPayload(String planetId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TravelToPlanetPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(AeronauticsSpace.MODID, "travel_to_planet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TravelToPlanetPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, TravelToPlanetPayload::planetId,
                    TravelToPlanetPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
