package io.icker.factions.config;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ItemsConfig {
    @SerializedName("requireItems")
    public boolean REQUIRE_ITEMS = false;

    @SerializedName("items")
    public Map<String, Integer> ITEMS = Map.of(
        "minecraft:diamond", 32,
        "minecraft:iron_block", 16,
        "minecraft:emerald", 16,
        "minecraft:gold_ingot", 32,
        "minecraft:ender_pearl", 16,
        "minecraft:obsidian", 32
    );
}
