package com.yuqi.aeronauticsspace;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * 服务端物理：让升到太空线以上的飞行器失重。
 *
 * 原理：Sable 每物理帧已经施加过重力，这里在重力之后补一个
 * 大小相等、方向相反的冲量（impulse = m * a * dt），把重力抵消掉。
 * 抵消比例 = spaceFactor(高度)，所以是渐变的。
 */
@EventBusSubscriber(modid = AeronauticsSpace.MODID)
public final class SpacePhysicsHandler {

    private SpacePhysicsHandler() {
    }

    @SubscribeEvent
    public static void onPostPhysicsTick(ForgeSablePostPhysicsTickEvent event) {
        final SubLevelPhysicsSystem system = event.getPhysicsSystem();
        final ServerLevel level = system.getLevel();

        // 只在主世界生效（后续可扩展成配置项）
        if (!Level.OVERWORLD.equals(level.dimension())) {
            return;
        }

        final double dt = event.getTimeStep();
        if (dt <= 0.0D) {
            return;
        }

        final SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (!(container instanceof ServerSubLevelContainer serverContainer)) {
            return;
        }

        for (final ServerSubLevel sub : serverContainer.getAllSubLevels()) {
            if (sub == null || sub.isRemoved()) {
                continue;
            }

            final Vector3dc position = sub.logicalPose().position();
            final double factor = SpaceAtmosphere.spaceFactor(position.y());
            if (factor <= 0.0D) {
                continue;
            }

            final RigidBodyHandle handle = RigidBodyHandle.of(sub);
            if (handle == null || !handle.isValid()) {
                continue;
            }

            final double mass = sub.getMassTracker().getMass();
            if (mass <= 0.0D) {
                continue;
            }

            // 该位置当前的重力加速度向量（一般为 (0, -9.8, 0) 量级）
            final Vector3d gravity = DimensionPhysicsData.getGravity(level, position);
            if (gravity.lengthSquared() <= 0.0D) {
                continue;
            }

            // 反向冲量：抵消 factor 比例的重力
            final Vector3d impulse = new Vector3d(gravity).mul(-mass * dt * factor);
            handle.applyLinearImpulse(impulse);
        }
    }
}
