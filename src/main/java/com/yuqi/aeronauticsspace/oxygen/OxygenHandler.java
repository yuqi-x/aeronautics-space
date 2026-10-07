package com.yuqi.aeronauticsspace.oxygen;

import com.yuqi.aeronauticsspace.AeronauticsSpace;
import com.yuqi.aeronauticsspace.planet.PlanetRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 氧气 / 窒息逻辑（服务端权威）。
 *
 *  - 在不透气的星球维度：氧气逐 tick 消耗，归零后开始窒息掉血
 *  - 在可呼吸维度（主世界 / 可呼吸星球）：氧气快速回充
 *  - 创造 / 旁观模式免疫
 */
@EventBusSubscriber(modid = AeronauticsSpace.MODID)
public final class OxygenHandler {

    /** 每 tick 消耗量（1 = 15 秒见底） */
    private static final int DRAIN_PER_TICK = 1;
    /** 可呼吸时每 tick 回充量 */
    private static final int REFILL_PER_TICK = 6;
    /** 窒息伤害间隔（tick） */
    private static final int SUFFOCATE_INTERVAL = 30;
    /** 每次窒息伤害 */
    private static final float SUFFOCATE_DAMAGE = 1.0F;

    private OxygenHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(final PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        // 判断当前维度大气是否可呼吸；非星球维度（主世界等）默认可呼吸
        final boolean breathable = PlanetRegistry.fromDimension(player.level().dimension())
                .map(PlanetRegistry.Planet::breathable)
                .orElse(true);

        final int current = player.getData(ModAttachments.OXYGEN.get());

        if (breathable) {
            if (current < ModAttachments.MAX_OXYGEN) {
                player.setData(ModAttachments.OXYGEN.get(),
                        Math.min(ModAttachments.MAX_OXYGEN, current + REFILL_PER_TICK));
            }
            if (player.getData(ModAttachments.SUFFOCATION.get()) != 0) {
                player.setData(ModAttachments.SUFFOCATION.get(), 0);
            }
            return;
        }

        // --- 无氧环境 ---
        final int next = Math.max(0, current - DRAIN_PER_TICK);
        player.setData(ModAttachments.OXYGEN.get(), next);

        if (next > 0) {
            player.setData(ModAttachments.SUFFOCATION.get(), 0);
            return;
        }

        // 氧气耗尽：累积窒息计时
        final int suff = player.getData(ModAttachments.SUFFOCATION.get()) + 1;
        if (suff >= SUFFOCATE_INTERVAL) {
            player.setData(ModAttachments.SUFFOCATION.get(), 0);
            player.hurt(player.damageSources().generic(), SUFFOCATE_DAMAGE);
        } else {
            player.setData(ModAttachments.SUFFOCATION.get(), suff);
        }
    }
}
