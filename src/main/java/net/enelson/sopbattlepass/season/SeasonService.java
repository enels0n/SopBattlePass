package net.enelson.sopbattlepass.season;

import net.enelson.sopbattlepass.SopBattlePass;
import net.enelson.sopbattlepass.config.PluginSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public final class SeasonService {

    private final SeasonDefinition activeSeason;

    public SeasonService(SopBattlePass plugin, PluginSettings settings) {
        String seasonId = settings.getActiveSeasonId();
        File file = new File(plugin.getDataFolder(), "seasons/" + seasonId + ".yml");
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection levels = configuration.getConfigurationSection("levels");
        Map<Integer, Integer> levelTable = new HashMap<Integer, Integer>();
        if (levels != null) {
            for (String key : levels.getKeys(false)) {
                ConfigurationSection levelSection = levels.getConfigurationSection(key);
                if (levelSection == null) {
                    continue;
                }
                int level = Integer.parseInt(key);
                int requiredXp = levelSection.getInt("required-total-xp", 0);
                levelTable.put(Integer.valueOf(level), Integer.valueOf(requiredXp));
            }
        }
        this.activeSeason = new SeasonDefinition(
                seasonId,
                configuration.getString("name", seasonId),
                LocalDateTime.parse(configuration.getString("starts-at")),
                LocalDateTime.parse(configuration.getString("ends-at")),
                levelTable
        );
    }

    public SeasonDefinition getActiveSeason() {
        return activeSeason;
    }
}
