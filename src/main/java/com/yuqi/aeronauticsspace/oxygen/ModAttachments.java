package com.yuqi.aeronauticsspace.oxygen;

import com.mojang.serialization.Codec;
import com.yuqi.aeronauticsspace.AeronauticsSpace;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** 玩家氧气值（NeoForge Data Attachment，自动同步到客户端供 HUD 用）。 */
public final class ModAttachments {

    /** 满氧 = 300 tick = 15 秒 */
    public static final int MAX_OXYGEN = 300;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AeronauticsSpace.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> OXYGEN =
            ATTACHMENTS.register("oxygen", () -> AttachmentType.builder(() -> MAX_OXYGEN)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    /** 累计在无氧环境中待了多久（tick），用于窒息伤害节奏 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> SUFFOCATION =
            ATTACHMENTS.register("suffocation", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
