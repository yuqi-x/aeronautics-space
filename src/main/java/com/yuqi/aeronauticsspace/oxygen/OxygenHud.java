package com.yuqi.aeronauticsspace.oxygen;

import com.yuqi.aeronauticsspace.AeronauticsSpace;
import com.yuqi.aeronauticsspace.planet.PlanetRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** 氧气 HUD：只在无氧环境显示。 */
@EventBusSubscriber(modid = AeronauticsSpace.MODID, value = Dist.CLIENT)
public final class OxygenHud {

    private static final int BAR_W = 82;
    private static final int BAR_H = 7;

    private OxygenHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(final RenderGuiEvent.Post event) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }

        // 只在无氧星球显示
        final boolean breathable = PlanetRegistry.fromDimension(mc.level.dimension())
                .map(PlanetRegistry.Planet::breathable)
                .orElse(true);
        if (breathable) {
            return;
        }

        final int oxygen = mc.player.getData(ModAttachments.OXYGEN.get());
        final float ratio = Math.max(0.0F, Math.min(1.0F,
                (float) oxygen / (float) ModAttachments.MAX_OXYGEN));

        final GuiGraphics g = event.getGuiGraphics();
        final int screenW = mc.getWindow().getGuiScaledWidth();
        final int screenH = mc.getWindow().getGuiScaledHeight();

        final int x = screenW / 2 + 92;
        final int y = screenH - 52;

        // 底槽
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, 0xA0000000);
        // 剩余氧气
        final int fillW = (int) (BAR_W * ratio);
        final int color = ratio > 0.5F ? 0xFF3FC7FF : (ratio > 0.2F ? 0xFFFFC13F : 0xFFFF4B3F);
        g.fill(x, y, x + fillW, y + BAR_H, color);

        // 文字
        g.drawString(mc.font, Component.literal("O₂"), x - 16, y - 2, 0xFFFFFF, true);
        if (oxygen <= 0) {
            final String warn = "§c窒息";
            g.drawString(mc.font, Component.literal(warn), x, y - 12, 0xFFFFFF, true);
        } else {
            final String txt = "§b" + (oxygen / 20) + "s";
            g.drawString(mc.font, Component.literal(txt), x + BAR_W + 4, y - 2, 0xFFFFFF, true);
        }
    }
}
