package com.fldsmdfr.command;

import com.fldsmdfr.config.ModConfig;
import com.fldsmdfr.core.BigNum;
import com.fldsmdfr.core.Fmt;
import com.fldsmdfr.tile.TileEntityFldsmdfr;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

/** /fldsmdfr config [key value] | info | evolve <log10> | surge | reset */
public class CommandFldsmdfr extends CommandBase {
    @Override
    public String getCommandName() { return "fldsmdfr"; }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fldsmdfr <config [chave valor] | info | evolve <expoente10> | surge>";
    }

    @Override
    public int getRequiredPermissionLevel() { return 2; }

    private static void say(ICommandSender s, String m) { s.addChatMessage(new ChatComponentText(m)); }

    private static TileEntityFldsmdfr nearest(ICommandSender s) {
        BlockPos p = s.getPosition();
        TileEntityFldsmdfr best = null;
        double bd = Double.MAX_VALUE;
        for (Object o : s.getEntityWorld().loadedTileEntityList) {
            if (o instanceof TileEntityFldsmdfr) {
                TileEntityFldsmdfr t = (TileEntityFldsmdfr) o;
                double d = t.getPos().distanceSq(p);
                if (d < bd) { bd = d; best = t; }
            }
        }
        return best;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) { say(sender, getCommandUsage(sender)); return; }
        String sub = args[0].toLowerCase();
        if (sub.equals("config")) {
            if (args.length == 1) {
                for (ModConfig.Setting s : ModConfig.all()) say(sender, s.key + " = " + s.valueString() + "  [" + s.min + ".." + s.max + "]");
            } else if (args.length == 2) {
                ModConfig.Setting s = ModConfig.get(args[1]);
                say(sender, s == null ? "Chave desconhecida" : s.key + " = " + s.valueString() + " - " + s.comment);
            } else {
                String err = ModConfig.set(args[1], args[2]);
                say(sender, err == null ? "OK: " + args[1] + " = " + ModConfig.get(args[1]).valueString() : err);
            }
            return;
        }
        TileEntityFldsmdfr te = nearest(sender);
        if (te == null) { say(sender, "Nenhuma FLDSMDFR carregada neste mundo."); return; }
        if (sub.equals("info")) {
            te.syncNow();
            say(sender, "Estado: " + te.view.stateName() + " | " + te.view.stageName() + " | Evolucao " + te.view.evolution.format()
                    + " | Componentes " + te.view.components.format() + " | Prod " + te.view.capacity.format()
                    + "/s | Comida " + String.format("%.2f", te.view.foodSize) + " blocos | Instab " + String.format("%.2f", te.view.instability));
        } else if (sub.equals("evolve") && args.length >= 2) {
            try {
                double lg = Double.parseDouble(args[1]);
                te.state.evolution = BigNum.fromLog10(lg);
                te.markDirty();
                say(sender, "Evolucao = 10^" + lg + " (o visual da maquina muda conforme a evolucao)");
            } catch (NumberFormatException e) {
                say(sender, "Numero invalido");
            }
        } else if (sub.equals("surge")) {
            te.state.surgeTicks = 400;
            say(sender, "Superproducao iniciada.");
        } else {
            say(sender, getCommandUsage(sender));
        }
    }
}
