package com.yuqi.aeronauticsspace.weather;

import com.yuqi.aeronauticsspace.planet.PlanetRegistry;

import java.util.Map;
import java.util.Optional;

/** 星球天气类型。 */
public enum PlanetWeather {

    /** 无天气（真空世界） */
    NONE("晴朗", 0xFFFFFF),
    /** 沙尘暴：火星 */
    DUST_STORM("沙尘暴", 0xC98A5E),
    /** 硫酸雨：金星 */
    ACID_RAIN("硫酸雨", 0xE8D26A),
    /** 极寒风暴：天王星 / 冥王星 */
    FROST_STORM("极寒风暴", 0x9FE3E8),
    /** 超级风暴：木星 / 海王星 */
    GREAT_STORM("超级风暴", 0x7A5FD9);

    private final String cnName;
    private final int color;

    PlanetWeather(String cnName, int color) {
        this.cnName = cnName;
        this.color = color;
    }

    public String cnName() {
        return cnName;
    }

    public int color() {
        return color;
    }

    private static final Map<String, PlanetWeather> BY_PLANET = Map.ofEntries(
            Map.entry("venus", ACID_RAIN),
            Map.entry("mars", DUST_STORM),
            Map.entry("jupiter", GREAT_STORM),
            Map.entry("uranus", FROST_STORM),
            Map.entry("neptune", GREAT_STORM),
            Map.entry("pluto", FROST_STORM)
    );

    /** 该星球有哪些天气。 */
    public static PlanetWeather of(PlanetRegistry.Planet planet) {
        return Optional.ofNullable(BY_PLANET.get(planet.id())).orElse(NONE);
    }

    /** 该星球在指定世界时刻是否正处于恶劣天气。 */
    public static boolean isActive(PlanetWeather weather, long gameTime) {
        if (weather == NONE) {
            return false;
        }
        // 每 3000 tick（2.5 分钟）为一个天气周期，其中 1/3 的时间处于恶劣天气
        final long phase = (gameTime / 3000L) % 3L;
        return phase == 0L;
    }
}
