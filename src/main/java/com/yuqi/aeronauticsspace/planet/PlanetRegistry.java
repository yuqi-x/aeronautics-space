package com.yuqi.aeronauticsspace.planet;

import com.yuqi.aeronauticsspace.AeronauticsSpace;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 星球注册表（服务端 / 客户端共用的静态数据）。
 *
 * 每个星球对应数据包里的一个独立维度：
 *   data/aeronautics_space/dimension/&lt;id&gt;.json
 *   data/aeronautics_space/dimension_type/&lt;id&gt;.json
 *   data/aeronautics_space/dimension_physics/&lt;id&gt;.json  （重力 / 气压）
 */
public final class PlanetRegistry {

    /**
     * @param id          维度路径名
     * @param cnName      中文名
     * @param enName      英文名
     * @param gravityRel  相对地球重力（地球 = 1.0）
     * @param breathable  大气是否可以呼吸（决定要不要氧气系统）
     * @param skyTop      天顶颜色 RGB
     * @param skyHorizon  地平线颜色 RGB
     * @param hasWeather  是否有天气现象
     * @param surfaceBlock 地表方块（同步给客户端做提示用）
     */
    public record Planet(String id, String cnName, String enName, double gravityRel,
                         boolean breathable, int skyTop, int skyHorizon,
                         boolean hasWeather, String surfaceBlock) {
    }

    private static final List<Planet> PLANETS = new ArrayList<>();
    private static final Map<String, Planet> BY_ID = new LinkedHashMap<>();

    private static void add(Planet p) {
        PLANETS.add(p);
        BY_ID.put(p.id(), p);
    }

    static {
        //                 id        中文     英文          重力比  可呼吸  天顶色     地平线色   天气   地表
        add(new Planet("moon",    "月球",   "Moon",      0.165D, false, 0x000000, 0x050510, false, "minecraft:end_stone"));
        add(new Planet("mercury", "水星",   "Mercury",   0.378D, false, 0x000000, 0x120C08, false, "minecraft:stone"));
        add(new Planet("venus",   "金星",   "Venus",     0.904D, false, 0xE8C87A, 0xC9A25A, true,  "minecraft:yellow_terracotta"));
        add(new Planet("mars",    "火星",   "Mars",      0.379D, false, 0xC98A5E, 0x8C4A2F, true,  "minecraft:red_sand"));
        add(new Planet("ceres",   "谷神星", "Ceres",     0.029D, false, 0x000000, 0x080810, false, "minecraft:andesite"));
        add(new Planet("jupiter", "木星",   "Jupiter",   2.528D, false, 0xD9B98A, 0xA0784F, true,  "minecraft:yellow_concrete"));
        add(new Planet("saturn",  "土星",   "Saturn",    1.065D, false, 0xE4D3A8, 0xBCA478, false, "minecraft:sand"));
        add(new Planet("uranus",  "天王星", "Uranus",    0.886D, false, 0x9FE3E8, 0x5FC6D6, true,  "minecraft:packed_ice"));
        add(new Planet("neptune", "海王星", "Neptune",   1.137D, false, 0x3B62D9, 0x1E3A9E, true,  "minecraft:blue_ice"));
        add(new Planet("pluto",   "冥王星", "Pluto",     0.063D, false, 0x000000, 0x0A0810, false, "minecraft:deepslate"));
    }

    private PlanetRegistry() {
    }

    public static List<Planet> all() {
        return List.copyOf(PLANETS);
    }

    public static Optional<Planet> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** 该星球对应的维度 Key。 */
    public static ResourceKey<Level> dimensionKey(String id) {
        return ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(AeronauticsSpace.MODID, id));
    }

    /** 判断给定维度是不是我们的星球维度。 */
    public static Optional<Planet> fromDimension(ResourceKey<Level> key) {
        if (!AeronauticsSpace.MODID.equals(key.location().getNamespace())) {
            return Optional.empty();
        }
        return byId(key.location().getPath());
    }
}
