package com.yuqi.aeronauticsspace.net;

import com.yuqi.aeronauticsspace.AeronauticsSpace;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务端 → 客户端：请打开星球选择界面。 */
public record OpenPlanetSelectorPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenPlanetSelectorPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(AeronauticsSpace.MODID, "open_planet_selector"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPlanetSelectorPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenPlanetSelectorPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
