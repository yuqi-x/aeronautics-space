package com.yuqi.aeronauticsspace.net;

import com.yuqi.aeronauticsspace.planet.PlanetRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** 跨维度传送逻辑。 */
public final class PlanetTravel {

    private PlanetTravel() {
    }

    /**
     * 把玩家送到目标星球维度。
     *
     * @return 是否成功
     */
    public static boolean travel(ServerPlayer player, String planetId) {
        final PlanetRegistry.Planet planet = PlanetRegistry.byId(planetId).orElse(null);
        if (planet == null) {
            player.sendSystemMessage(Component.literal("§c未知星球: " + planetId));
            return false;
        }

        final MinecraftServer server = player.server;
        final ServerLevel target = server.getLevel(PlanetRegistry.dimensionKey(planetId));
        if (target == null) {
            player.sendSystemMessage(Component.literal("§c维度未加载: " + planetId + "（检查数据包）"));
            return false;
        }

        // 落点：出生点附近，取地表高度
        BlockPos base = target.getSharedSpawnPos();
        final int chunkX = base.getX();
        final int chunkZ = base.getZ();
        // 强制加载目标区块，否则高度图会是空的
        target.getChunk(chunkX >> 4, chunkZ >> 4);
        final int surfaceY = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, chunkX, chunkZ);

        final double x = chunkX + 0.5D;
        final double y = surfaceY + 1.0D;
        final double z = chunkZ + 0.5D;

        player.teleportTo(target, x, y, z, player.getYRot(), player.getXRot());
        final String gravityTag = String.format("%.2f", planet.gravityRel());
        player.sendSystemMessage(Component.literal(
                "§b已抵达 §f" + planet.cnName() + " §7（" + planet.enName() + "）§8· 重力 " + gravityTag + "g"
                        + (planet.breathable() ? " · 空气可呼吸" : " · §c无氧环境")));
        return true;
    }

    /** 供命令 / 其它模块使用：判断某维度是否已是星球维度。 */
    public static boolean isPlanetDimension(Level level) {
        return PlanetRegistry.fromDimension(level.dimension()).isPresent();
    }
}
