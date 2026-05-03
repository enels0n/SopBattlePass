package net.enelson.sopbattlepass.placeholder;

import net.enelson.sopbattlepass.SopBattlePass;
import net.enelson.sopbattlepass.config.PluginSettings;
import net.enelson.sopbattlepass.player.PlayerBattlePassProgress;
import net.enelson.sopbattlepass.player.PlayerProgressService;
import net.enelson.sopbattlepass.season.SeasonDefinition;
import net.enelson.sopbattlepass.season.SeasonService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Locale;

public final class BattlePassPlaceholders {

    private final SopBattlePass plugin;
    private final SeasonService seasonService;
    private final PlayerProgressService playerProgressService;
    private final PluginSettings settings;

    public BattlePassPlaceholders(SopBattlePass plugin,
                                  SeasonService seasonService,
                                  PlayerProgressService playerProgressService,
                                  PluginSettings settings) {
        this.plugin = plugin;
        this.seasonService = seasonService;
        this.playerProgressService = playerProgressService;
        this.settings = settings;
    }

    public String resolveMessage(String input) {
        return color(input.replace("{prefix}", plugin.getConfig().getString("messages.prefix", "")));
    }

    public String resolve(Player player, String input) {
        String resolved = input.replace("{prefix}", plugin.getConfig().getString("messages.prefix", ""));
        try {
            PlayerBattlePassProgress progress = playerProgressService.getProgress(player);
            SeasonDefinition season = seasonService.getActiveSeason();
            resolved = resolved
                    .replace("{season_name}", season.getName())
                    .replace("{level}", Integer.toString(progress.getLevel()))
                    .replace("{xp}", Integer.toString(progress.getXp()))
                    .replace("{premium}", progress.isPremiumActive() ? "true" : "false")
                    .replace("{premium_status}", progress.isPremiumActive() ? "enabled" : "locked")
                    .replace("{server_id}", settings.getServerIdentity().getServerId())
                    .replace("{server_group}", settings.getServerIdentity().getServerGroup());
        } catch (SQLException exception) {
            plugin.getLogger().warning("Unable to resolve placeholders: " + exception.getMessage());
        }

        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                resolved = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, resolved);
            } catch (Throwable ignored) {
            }
        }
        return color(resolved);
    }

    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }

        try {
            PlayerBattlePassProgress progress = playerProgressService.getProgress(player);
            String normalized = params.toLowerCase(Locale.ROOT);
            if ("season_name".equals(normalized)) {
                return seasonService.getActiveSeason().getName();
            }
            if ("level".equals(normalized)) {
                return Integer.toString(progress.getLevel());
            }
            if ("xp".equals(normalized)) {
                return Integer.toString(progress.getXp());
            }
            if ("premium".equals(normalized)) {
                return progress.isPremiumActive() ? "true" : "false";
            }
            if ("server_id".equals(normalized)) {
                return settings.getServerIdentity().getServerId();
            }
            if ("server_group".equals(normalized)) {
                return settings.getServerIdentity().getServerGroup();
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Placeholder request failed: " + exception.getMessage());
        }

        return "";
    }

    private String color(String input) {
        return plugin.getTextUtils().color(input);
    }
}
