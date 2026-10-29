package com.fldsmdfr.config;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/** All editable settings (file config/fldsmdfr.cfg or in game with /fldsmdfr config <key> <value>). */
public final class ModConfig {
    public static final class Setting {
        public final String key, comment;
        public final double def, min, max;
        public final boolean isBool, isInt;
        public volatile double value;

        Setting(String key, double def, double min, double max, boolean isBool, boolean isInt, String comment) {
            this.key = key;
            this.def = def;
            this.min = min;
            this.max = max;
            this.isBool = isBool;
            this.isInt = isInt;
            this.comment = comment;
            this.value = def;
        }

        public String valueString() {
            if (isBool) return value != 0 ? "true" : "false";
            if (isInt) return Long.toString((long) value);
            return Double.toString(value);
        }
    }

    private static final Map<String, Setting> S = new LinkedHashMap<String, Setting>();
    private static Configuration cfg;
    private static final String CAT = "general";

    static {
        num("evolutionSpeed", 1.0, 0.0, 100000.0, "Multiplier of the evolution speed (1 = slow, 1000 = food catastrophe).");
        num("productionIntensity", 1.0, 0.0, 100000.0, "Multiplier of food production.");
        num("stormIntensity", 1.0, 0.0, 1000.0, "Multiplier of launch volumes and surge frequency.");
        num("instabilityRate", 1.0, 0.0, 100000.0, "Multiplier of instability growth.");
        num("launchDistance", 48.0, 8.0, 400.0, "Base launch range in blocks (machine size and launchers add to it).");
        num("foodSizeMultiplier", 1.0, 0.1, 20.0, "Multiplier of the physical size of the launched food.");
        integer("maxActiveFood", 150, 0, 3000, "Maximum number of food entities alive per world. Oldest are removed.");
        num("simulationDistance", 96.0, 16.0, 512.0, "Food entities are spawned only when a player is within this distance.");
        integer("updateInterval", 2, 1, 40, "Ticks between machine logic updates.");
        integer("syncInterval", 20, 5, 200, "Ticks between client synchronisations (small packet).");
        bool("offlineProgress", true, "Apply logical progression for the time the world was closed.");
        num("maxOfflineHours", 72.0, 0.0, 8760.0, "Maximum hours of offline progression applied at once.");
        bool("visualEffects", true, "Sparks, smoke and electric arcs.");
        bool("sounds", true, "Mechanical sounds.");
        integer("maxLaunchesPerUpdate", 6, 0, 200, "Maximum food entities spawned per launch cycle.");
        integer("visualComponents", 220, 0, 2000, "Maximum number of component pods drawn on the machine (level of detail).");
        integer("foodLifespanTicks", 3600, 200, 24000, "Ticks before food on the ground disappears.");
    }

    private ModConfig() {}

    private static void num(String k, double d, double mn, double mx, String c) { S.put(k, new Setting(k, d, mn, mx, false, false, c)); }
    private static void integer(String k, int d, int mn, int mx, String c) { S.put(k, new Setting(k, d, mn, mx, false, true, c)); }
    private static void bool(String k, boolean d, String c) { S.put(k, new Setting(k, d ? 1 : 0, 0, 1, true, false, c)); }

    public static void load(File file) {
        cfg = new Configuration(file);
        cfg.load();
        for (Setting s : S.values()) {
            if (s.isBool) {
                s.value = cfg.get(CAT, s.key, s.def != 0, s.comment).getBoolean() ? 1 : 0;
            } else if (s.isInt) {
                s.value = cfg.get(CAT, s.key, (int) s.def, s.comment, (int) s.min, (int) s.max).getInt();
            } else {
                s.value = cfg.get(CAT, s.key, s.def, s.comment, s.min, s.max).getDouble();
            }
        }
        if (cfg.hasChanged()) cfg.save();
    }

    public static Collection<Setting> all() { return S.values(); }

    public static Setting get(String key) { return S.get(key); }

    public static double d(String key) { return S.get(key).value; }

    public static int i(String key) { return (int) S.get(key).value; }

    public static boolean b(String key) { return S.get(key).value != 0; }

    /** Sets and persists a value. Returns an error message or null on success. */
    public static String set(String key, String text) {
        Setting s = S.get(key);
        if (s == null) return "Unknown setting: " + key;
        double v;
        try {
            if (s.isBool) {
                if (text.equalsIgnoreCase("true") || text.equals("1") || text.equalsIgnoreCase("on")) v = 1;
                else if (text.equalsIgnoreCase("false") || text.equals("0") || text.equalsIgnoreCase("off")) v = 0;
                else return "Use true/false";
            } else {
                v = Double.parseDouble(text);
                if (Double.isNaN(v) || Double.isInfinite(v)) return "Invalid number";
                if (s.isInt) v = Math.round(v);
            }
        } catch (NumberFormatException ex) {
            return "Invalid number: " + text;
        }
        if (v < s.min || v > s.max) return "Out of range [" + s.min + ", " + s.max + "]";
        s.value = v;
        if (cfg != null) {
            Property p = cfg.getCategory(CAT).get(key);
            if (p != null) {
                if (s.isBool) p.set(v != 0);
                else if (s.isInt) p.set((int) v);
                else p.set(v);
            }
            cfg.save();
        }
        return null;
    }
}
