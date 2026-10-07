package com.yuqi.aeronauticsspace.weather;

import com.yuqi.aeronauticsspace.AeronauticsSpace;
import com.yuqi.aeronauticsspace.planet.PlanetRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Optional;

/**
 * 星球天气效果（服务端）。
 *
 * 每 40 tick 检查一次：若当前维度是星球维度、且该星球正处于恶劣天气，
 * 就给维度内玩家施加对应效果。
 */
@EventBusSubscriber(modid = AeronauticsSpace.MODID)
public final class WeatherHandler {

    private static final int CHECK_INTERVAL = 40;
    private static final int EFFECT_DURATION = 100;

    private WeatherHandler() {
    }

    @SubscribeEvent
    public static void onLevelTick(final LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        final Optional<PlanetRegistry.Planet> planetOpt = PlanetRegistry.fromDimension(level.dimension());
        if (planetOpt.isEmpty()) {
            return;
        }

        final PlanetWeather weather = PlanetWeather.of(planetOpt.get());
        if (weather == PlanetWeather.NONE) {
            return;
        }

        final long gameTime = level.getGameTime();
        if (gameTime % CHECK_INTERVAL != 0L) {
            return;
        }
        if (!PlanetWeather.isActive(weather, gameTime)) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            applyWeather(player, weather);
        }
    }

    private static void applyWeather(ServerPlayer player, PlanetWeather weather) {
        switch (weather) {
            case DUST_STORM -> {
                // 沙尘暴：行动迟缓 + 视线受阻（挖掘疲劳）
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, EFFECT_DURATION, 0, false, false));
                // 每 5 次检查（约 10 秒）吹一次
                if (player.tickCount % 200 == 0) {
                    player.push(0.6D, 0.12D, 0.6D);
                }
            }
            case ACID_RAIN -> {
                // 硫酸雨：持续腐蚀伤害
                if (player.tickCount % 40 == 0) {
                    player.hurt(player.damageSources().generic(), 1.0F);
                }
            }
            case FROST_STORM -> {
                // 极寒：缓慢 + 寒冷伤害
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION, 1, false, false));
                if (player.tickCount % 60 == 0) {
                    player.hurt(player.damageSources().freeze(), 1.0F);
                }
            }
            case GREAT_STORM -> {
                // 超级风暴：强力击退流
                final Vec3 look = player.getLookAngle();
                player.push(look.x * 1.2D, 0.35D, look.z * 1.2D);
                player.hurtMarked = true;
                if (player.tickCount % 100 == 0) {
                    player.hurt(player.damageSources().generic(), 1.0F);
                }
            }
            default -> {
            }
        }
    }
}
