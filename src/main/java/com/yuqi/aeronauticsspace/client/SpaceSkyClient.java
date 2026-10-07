package com.yuqi.aeronauticsspace.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yuqi.aeronauticsspace.AeronauticsSpace;
import com.yuqi.aeronauticsspace.SpaceAtmosphere;
import com.yuqi.aeronauticsspace.planet.PlanetRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

import java.util.Optional;

/**
 * 客户端太空 / 行星天空渲染。
 *
 * 两种模式：
 *  1. 主世界高空（y >= 太空线）—— 星空天球（真贴图）+ 太阳 / 月亮圆盘
 *  2. 星球维度 —— 按该星球配置渲染：无大气 = 星空天球；有大气 = 天顶→地平线渐变穹顶
 *
 * 贴图素材来自 Solar System Scope（CC BY 4.0）。
 */
@EventBusSubscriber(modid = AeronauticsSpace.MODID, value = Dist.CLIENT)
public final class SpaceSkyClient {

    private static final ResourceLocation TEX_STARS =
            ResourceLocation.fromNamespaceAndPath(AeronauticsSpace.MODID, "textures/environment/space_stars.png");
    private static final ResourceLocation TEX_SUN =
            ResourceLocation.fromNamespaceAndPath(AeronauticsSpace.MODID, "textures/environment/space_sun.png");
    private static final ResourceLocation TEX_MOON =
            ResourceLocation.fromNamespaceAndPath(AeronauticsSpace.MODID, "textures/environment/space_moon.png");

    private static final int LON_SEG = 72;
    private static final int LAT_SEG = 36;
    private static final float DOME_RADIUS = 170.0F;
    private static final float BODY_DIST = 150.0F;
    private static final float SUN_SIZE = 13.0F;
    private static final float MOON_SIZE = 9.0F;

    private SpaceSkyClient() {
    }

    /** 主世界高空：0..1 的"太空程度" */
    public static double clientSpaceFactor() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0.0D;
        }
        return SpaceAtmosphere.spaceFactor(mc.player.getY());
    }

    /** 当前所在星球（不在星球维度则为空） */
    private static Optional<PlanetRegistry.Planet> currentPlanet() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return Optional.empty();
        }
        return PlanetRegistry.fromDimension(mc.level.dimension());
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        final Optional<PlanetRegistry.Planet> planet = currentPlanet();
        if (planet.isPresent()) {
            // 行星大气：雾色取该星球的"地平线色"偏暗版
            final int c = planet.get().skyHorizon();
            final float r = ((c >> 16) & 0xFF) / 255.0F;
            final float g = ((c >> 8) & 0xFF) / 255.0F;
            final float b = (c & 0xFF) / 255.0F;
            event.setRed(r * 0.85F);
            event.setGreen(g * 0.85F);
            event.setBlue(b * 0.85F);
            return;
        }

        final double f = clientSpaceFactor();
        if (f <= 0.0D) {
            return;
        }
        final float t = (float) f;
        event.setRed(lerp(event.getRed(), 0.0F, t));
        event.setGreen(lerp(event.getGreen(), 0.0F, t));
        event.setBlue(lerp(event.getBlue(), 0.035F, t));
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        final PoseStack pose = event.getPoseStack();
        final Camera camera = event.getCamera();
        final Vec3 cam = camera.getPosition();
        final Tesselator tess = Tesselator.getInstance();

        final Optional<PlanetRegistry.Planet> planetOpt = currentPlanet();
        if (planetOpt.isPresent()) {
            renderPlanetSky(pose, cam, tess, planetOpt.get());
            return;
        }

        // --- 主世界高空 ---
        final double f = clientSpaceFactor();
        if (f <= 0.02D) {
            return;
        }
        renderSpaceDome(pose, cam, tess, (float) f);
    }

    /** 主世界高空：星空天球 + 太阳 / 月亮 */
    private static void renderSpaceDome(PoseStack pose, Vec3 cam, Tesselator tess, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        final Matrix4f mat = pose.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEX_STARS);
        final BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int i = 0; i < LAT_SEG; i++) {
            final double lat0 = Math.PI * ((double) i / LAT_SEG - 0.5D);
            final double lat1 = Math.PI * ((double) (i + 1) / LAT_SEG - 0.5D);
            final float v0 = 1.0F - (float) i / LAT_SEG;
            final float v1 = 1.0F - (float) (i + 1) / LAT_SEG;
            final float y0 = (float) (Math.sin(lat0) * DOME_RADIUS);
            final float y1 = (float) (Math.sin(lat1) * DOME_RADIUS);
            final float r0 = (float) (Math.cos(lat0) * DOME_RADIUS);
            final float r1 = (float) (Math.cos(lat1) * DOME_RADIUS);
            for (int j = 0; j < LON_SEG; j++) {
                final double lon0 = 2.0D * Math.PI * (double) j / LON_SEG;
                final double lon1 = 2.0D * Math.PI * (double) (j + 1) / LON_SEG;
                final float u0 = (float) j / LON_SEG;
                final float u1 = (float) (j + 1) / LON_SEG;
                final float c0 = (float) Math.cos(lon0);
                final float s0 = (float) Math.sin(lon0);
                final float c1 = (float) Math.cos(lon1);
                final float s1 = (float) Math.sin(lon1);
                buf.addVertex(mat, r0 * c0, y0, r0 * s0).setUv(u0, v0);
                buf.addVertex(mat, r0 * c1, y0, r0 * s1).setUv(u1, v0);
                buf.addVertex(mat, r1 * c1, y1, r1 * s1).setUv(u1, v1);
                buf.addVertex(mat, r1 * c0, y1, r1 * s0).setUv(u0, v1);
            }
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());

        renderDisc(tess, mat, TEX_SUN, 0.35F, 0.62F, 0.70F, SUN_SIZE, 1.0F, alpha);
        renderDisc(tess, mat, TEX_MOON, -0.35F, 0.62F, -0.70F, MOON_SIZE, 0.92F, alpha);

        pose.popPose();
        restoreState();
    }

    /** 星球维度天空 */
    private static void renderPlanetSky(PoseStack pose, Vec3 cam, Tesselator tess, PlanetRegistry.Planet planet) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        final Matrix4f mat = pose.last().pose();

        // 该星球"有没有大气"用天顶色判断：纯黑 = 真空 = 走星空
        final boolean vacuum = (planet.skyTop() & 0xFFFFFF) == 0;

        if (vacuum) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TEX_STARS);
            drawStarDome(tess, mat);
            renderDisc(tess, mat, TEX_SUN, 0.35F, 0.62F, 0.70F, SUN_SIZE, 1.0F, 1.0F);
            // 真空星球：远处挂一颗该星球"母星"的观感 —— 用月亮贴图充当
            renderDisc(tess, mat, TEX_MOON, -0.45F, 0.45F, -0.75F, MOON_SIZE * 1.6F, 0.85F, 1.0F);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            drawGradientDome(tess, mat, planet.skyTop(), planet.skyHorizon());
        }

        pose.popPose();
        restoreState();
    }

    /** 星空天球（复用几何） */
    private static void drawStarDome(Tesselator tess, Matrix4f mat) {
        final BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int i = 0; i < LAT_SEG; i++) {
            final double lat0 = Math.PI * ((double) i / LAT_SEG - 0.5D);
            final double lat1 = Math.PI * ((double) (i + 1) / LAT_SEG - 0.5D);
            final float v0 = 1.0F - (float) i / LAT_SEG;
            final float v1 = 1.0F - (float) (i + 1) / LAT_SEG;
            final float y0 = (float) (Math.sin(lat0) * DOME_RADIUS);
            final float y1 = (float) (Math.sin(lat1) * DOME_RADIUS);
            final float r0 = (float) (Math.cos(lat0) * DOME_RADIUS);
            final float r1 = (float) (Math.cos(lat1) * DOME_RADIUS);
            for (int j = 0; j < LON_SEG; j++) {
                final double lon0 = 2.0D * Math.PI * (double) j / LON_SEG;
                final double lon1 = 2.0D * Math.PI * (double) (j + 1) / LON_SEG;
                final float u0 = (float) j / LON_SEG;
                final float u1 = (float) (j + 1) / LON_SEG;
                final float c0 = (float) Math.cos(lon0);
                final float s0 = (float) Math.sin(lon0);
                final float c1 = (float) Math.cos(lon1);
                final float s1 = (float) Math.sin(lon1);
                buf.addVertex(mat, r0 * c0, y0, r0 * s0).setUv(u0, v0);
                buf.addVertex(mat, r0 * c1, y0, r0 * s1).setUv(u1, v0);
                buf.addVertex(mat, r1 * c1, y1, r1 * s1).setUv(u1, v1);
                buf.addVertex(mat, r1 * c0, y1, r1 * s0).setUv(u0, v1);
            }
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
    }

    /** 大气渐变穹顶：天顶色 -> 地平线色 -> 下方压暗 */
    private static void drawGradientDome(Tesselator tess, Matrix4f mat, int topRgb, int horizonRgb) {
        final float tr = ((topRgb >> 16) & 0xFF) / 255.0F;
        final float tg = ((topRgb >> 8) & 0xFF) / 255.0F;
        final float tb = (topRgb & 0xFF) / 255.0F;
        final float hr = ((horizonRgb >> 16) & 0xFF) / 255.0F;
        final float hg = ((horizonRgb >> 8) & 0xFF) / 255.0F;
        final float hb = (horizonRgb & 0xFF) / 255.0F;

        final BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < LAT_SEG; i++) {
            // t: 0 = 天顶, 1 = 地平线, >1 = 地平线以下
            final double t0 = (double) i / LAT_SEG;      // i=LAT/2 处为地平线
            final double t1 = (double) (i + 1) / LAT_SEG;
            final double lat0 = Math.PI * (t0 - 0.5D);
            final double lat1 = Math.PI * (t1 - 0.5D);
            final float[] c0 = skyColorAt(t0, tr, tg, tb, hr, hg, hb);
            final float[] c1 = skyColorAt(t1, tr, tg, tb, hr, hg, hb);
            final float y0 = (float) (Math.sin(lat0) * DOME_RADIUS);
            final float y1 = (float) (Math.sin(lat1) * DOME_RADIUS);
            final float r0 = (float) (Math.cos(lat0) * DOME_RADIUS);
            final float r1 = (float) (Math.cos(lat1) * DOME_RADIUS);
            for (int j = 0; j < LON_SEG; j++) {
                final double lon0 = 2.0D * Math.PI * (double) j / LON_SEG;
                final double lon1 = 2.0D * Math.PI * (double) (j + 1) / LON_SEG;
                final float cs0 = (float) Math.cos(lon0);
                final float sn0 = (float) Math.sin(lon0);
                final float cs1 = (float) Math.cos(lon1);
                final float sn1 = (float) Math.sin(lon1);
                buf.addVertex(mat, r0 * cs0, y0, r0 * sn0).setColor(c0[0], c0[1], c0[2], 1.0F);
                buf.addVertex(mat, r0 * cs1, y0, r0 * sn1).setColor(c0[0], c0[1], c0[2], 1.0F);
                buf.addVertex(mat, r1 * cs1, y1, r1 * sn1).setColor(c1[0], c1[1], c1[2], 1.0F);
                buf.addVertex(mat, r1 * cs0, y1, r1 * sn0).setColor(c1[0], c1[1], c1[2], 1.0F);
            }
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
    }

    /** t=0 天顶，t=0.5 地平线，t>0.5 地下（压暗） */
    private static float[] skyColorAt(double t, float tr, float tg, float tb,
                                      float hr, float hg, float hb) {
        if (t <= 0.5D) {
            final float k = (float) (t / 0.5D);
            return new float[]{lerp(tr, hr, k), lerp(tg, hg, k), lerp(tb, hb, k)};
        }
        final float k = (float) Math.min(1.0D, (t - 0.5D) / 0.5D);
        final float dark = 1.0F - 0.75F * k;
        return new float[]{hr * dark, hg * dark, hb * dark};
    }

    /** 在给定方向画一个朝向相机的圆形贴图 */
    private static void renderDisc(Tesselator tess, Matrix4f mat, ResourceLocation tex,
                                   float dx, float dy, float dz, float size, float bright, float alpha) {
        final double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        final float ux = (float) (dx / len), uy = (float) (dy / len), uz = (float) (dz / len);
        float ax = -uy, ay = ux, az = 0.0F;
        float al = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (al < 1.0E-4F) {
            ax = 1.0F; ay = 0.0F; az = 0.0F; al = 1.0F;
        }
        ax /= al; ay /= al; az /= al;
        final float bx = uy * az - uz * ay;
        final float by = uz * ax - ux * az;
        final float bz = ux * ay - uy * ax;

        final float cx = ux * BODY_DIST, cy = uy * BODY_DIST, cz = uz * BODY_DIST;
        final float s = size;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(bright, bright, bright, alpha);

        final BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(mat, cx - ax * s - bx * s, cy - ay * s - by * s, cz - az * s - bz * s).setUv(0.0F, 1.0F);
        buf.addVertex(mat, cx + ax * s - bx * s, cy + ay * s - by * s, cz + az * s - bz * s).setUv(1.0F, 1.0F);
        buf.addVertex(mat, cx + ax * s + bx * s, cy + ay * s + by * s, cz + az * s + bz * s).setUv(1.0F, 0.0F);
        buf.addVertex(mat, cx - ax * s + bx * s, cy - ay * s + by * s, cz - az * s + bz * s).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(buf.buildOrThrow());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void restoreState() {
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
