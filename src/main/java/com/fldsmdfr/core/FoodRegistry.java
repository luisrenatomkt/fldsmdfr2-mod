package com.fldsmdfr.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Extensible list of physical food types (model index = list index, drawn by client.FoodModels). */
public final class FoodRegistry {
    public static final class FoodDef {
        public final String id;
        public final float width;   // collision width per unit of scale
        public final float height;  // collision height per unit of scale

        FoodDef(String id, float width, float height) {
            this.id = id;
            this.width = width;
            this.height = height;
        }
    }

    private static final List<FoodDef> DEFS = new ArrayList<FoodDef>();

    static {
        register("burger", 1.0f, 0.55f);
        register("pizza", 1.1f, 0.15f);
        register("donut", 0.85f, 0.35f);
        register("cake", 1.0f, 0.55f);
        register("pasta", 1.1f, 0.4f);
        register("hotdog", 1.1f, 0.4f);
        register("sandwich", 1.0f, 0.35f);
        register("fries", 0.8f, 0.8f);
        register("icecream", 0.6f, 1.0f);
    }

    private FoodRegistry() {}

    public static synchronized FoodDef register(String id, float width, float height) {
        FoodDef d = new FoodDef(id, width, height);
        DEFS.add(d);
        return d;
    }

    public static synchronized int count() { return DEFS.size(); }

    public static synchronized FoodDef get(int i) { return DEFS.get(Math.floorMod(i, DEFS.size())); }

    public static synchronized List<FoodDef> all() { return Collections.unmodifiableList(new ArrayList<FoodDef>(DEFS)); }
}
