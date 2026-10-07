package com.yuqi.aeronauticsspace.weather;

import com.yuqi.aeronauticsspace.AeronauticsSpace;
import com.yuqi.aeronauticsspace.planet.PlanetRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.Optional;

/** 星球天气 HUD 提示 + 全屏色调。 */
@EventBusSubscriber(modid = AeronauticsSpace.MODID, value = Dist.CLIENT)
public final class WeatherHud {

    private WeatherHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(final RenderGuiEvent.Post event) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) {
            return;
        }

        final Optional<PlanetRegistry.Planet> planetOpt = PlanetRegistry.fromDimension(mc.level.dimension());
        if (planetOpt.isEmpty()) {
            return;
        }

        final PlanetWeather weather = PlanetWeather.of(planetOpt.get());
        if (weather == PlanetWeather.NONE) {
            return;
        }
        if (!PlanetWeather.isActive(weather, mc.level.getGameTime())) {
            return;
        }

        final GuiGraphics g = event.getGuiGraphics();
        final int w = mc.getWindow().getGuiScaledWidth();
        final int h = mc.getWindow().getGuiScaledHeight();

        // 全屏色调（低透明度，营造天气氛围）
        final int base = weather.color();
        final int tint = (0x28 << 24) | (base & 0xFFFFFF);
        g.fill(0, 0, w, h, tint);

        // 左上角天气标签
        final String text = "§e⚠ " + weather.cnName();
        g.drawString(mc.font, Component.literal(text), 8, 8, 0xFFFFFF, true);
    }
}
