package com.yuqi.aeronauticsspace.net;

import com.yuqi.aeronauticsspace.planet.PlanetRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 星球选择界面。
 *
 * 由星际传送器右键打开（服务端发 OpenPlanetSelectorPayload）。
 * 点选星球 -> 发 TravelToPlanetPayload -> 服务端执行跨维度传送。
 */
public class PlanetSelectScreen extends Screen {

    private static final int COLS = 2;
    private static final int BTN_W = 178;
    private static final int BTN_H = 20;
    private static final int GAP_X = 10;
    private static final int GAP_Y = 6;

    public PlanetSelectScreen() {
        super(Component.translatable("gui.aeronautics_space.planet_select.title"));
    }

    @Override
    protected void init() {
        final List<PlanetRegistry.Planet> planets = PlanetRegistry.all();
        final int rows = (planets.size() + COLS - 1) / COLS;
        final int totalW = COLS * BTN_W + (COLS - 1) * GAP_X;

        final int startX = Math.max(4, (this.width - totalW) / 2);
        final int blockH = rows * (BTN_H + GAP_Y) - GAP_Y;
        final int startY = Math.max(46, (this.height - blockH) / 2 + 10);

        for (int i = 0; i < planets.size(); i++) {
            final PlanetRegistry.Planet p = planets.get(i);
            final int col = i % COLS;
            final int row = i / COLS;
            final int x = startX + col * (BTN_W + GAP_X);
            final int y = startY + row * (BTN_H + GAP_Y);

            final String label = p.cnName() + "  §7" + p.enName()
                    + "  §8· " + String.format("%.2f", p.gravityRel()) + "g"
                    + (p.breathable() ? "" : " §c✖O₂");

            this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                ModNetwork.sendTravelRequest(p.id());
                this.onClose();
            }).bounds(x, y, BTN_W, BTN_H).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 22, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.literal("§8选择目的地 · 重力与大气已标注"), this.width / 2, 36, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
