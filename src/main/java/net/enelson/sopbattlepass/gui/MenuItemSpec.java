package net.enelson.sopbattlepass.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MenuItemSpec {

    private final Material material;
    private final String name;
    private final Integer customModelData;
    private final List<String> lore;

    public MenuItemSpec(Material material, String name, Integer customModelData, List<String> lore) {
        this.material = material;
        this.name = name;
        this.customModelData = customModelData;
        this.lore = Collections.unmodifiableList(new ArrayList<String>(lore));
    }

    public static MenuItemSpec fromSection(ConfigurationSection section, Material fallbackMaterial, String fallbackName, List<String> fallbackLore) {
        if (section == null) {
            return new MenuItemSpec(fallbackMaterial, fallbackName, null, fallbackLore);
        }
        Material material = Material.matchMaterial(section.getString("material", fallbackMaterial.name()));
        if (material == null) {
            material = fallbackMaterial;
        }
        Integer customModelData = section.contains("custom-model-data") ? Integer.valueOf(section.getInt("custom-model-data")) : null;
        return new MenuItemSpec(
                material,
                section.getString("name", fallbackName),
                customModelData,
                new ArrayList<String>(section.getStringList("lore"))
        );
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public List<String> getLore() {
        return lore;
    }
}
