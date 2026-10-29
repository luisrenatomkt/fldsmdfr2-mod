package com.fldsmdfr.gui;

import com.fldsmdfr.core.Fmt;
import com.fldsmdfr.core.MachineView;
import com.fldsmdfr.tile.TileEntityFldsmdfr;

import net.minecraft.client.gui.inventory.GuiContainer;

/** Read-only status panel. Data comes from the small snapshot the server syncs periodically. */
public class GuiInfo extends GuiContainer {
    private final TileEntityFldsmdfr te;

    public GuiInfo(TileEntityFldsmdfr te) {
        super(new ContainerInfo());
        this.te = te;
        this.xSize = 250;
        this.ySize = 214;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int l = guiLeft, t = guiTop;
        drawRect(l, t, l + xSize, t + ySize, 0xDD101822);
        drawRect(l, t, l + xSize, t + 16, 0xFFE8731A);
        drawRect(l + 1, t + 17, l + xSize - 1, t + ySize - 1, 0xCC1B2733);
        MachineView v = te.view;
        fontRendererObj.drawStringWithShadow("FLDSMDFR - Painel de controle", l + 6, t + 4, 0xFFFFFF);

        String[][] rows = {
                {"Estado", v.stateName()},
                {"Estagio", v.stageName()},
                {"Tempo de funcionamento", Fmt.time(v.runtime)},
                {"Evolucao", v.evolution.format() + "  (10^" + String.format("%.1f", v.evolution.log10()) + ")"},
                {"Componentes", v.components.format() + "  (desenhados " + v.rendered + ")"},
                {"Tamanho do corpo", String.format("%.1fx  (~%.0f blocos de altura)", v.scale, v.scale * 2.2)},
                {"Tamanho da comida", String.format("%.2f blocos", v.foodSize)},
                {"Capacidade estimada", v.capacity.format() + " alimentos/s"},
                {"Alimentos produzidos", v.produced.format()},
                {"Lancadores", v.launchers.format()},
                {"Entidades de comida", v.activeFood + " / " + v.maxActive},
        };
        int y = t + 24;
        for (String[] r : rows) {
            fontRendererObj.drawString(r[0] + ":", l + 8, y, 0xFFB070);
            fontRendererObj.drawString(r[1], l + 8 + 112, y, 0xE8E8E8);
            y += 14;
        }
        // instability bar
        double sat = v.sat();
        fontRendererObj.drawString("Instabilidade: " + String.format("%.2f", v.instability), l + 8, y + 2, 0xFFB070);
        int bx = l + 8, by = y + 14, bw = xSize - 16;
        drawRect(bx - 1, by - 1, bx + bw + 1, by + 9, 0xFF000000);
        int fill = (int) (bw * Math.min(1.0, sat));
        int r = (int) (255 * Math.min(1.0, 0.3 + sat)), g = (int) (200 * (1.0 - sat));
        drawRect(bx, by, bx + fill, by + 8, 0xFF000000 | (r << 16) | (g << 8) | 0x30);
        fontRendererObj.drawString("Shift + clique direito (mao vazia) liga/desliga", l + 8, t + ySize - 14, 0x889999);
    }
}
