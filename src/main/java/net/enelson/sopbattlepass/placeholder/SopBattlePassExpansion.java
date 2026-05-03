package net.enelson.sopbattlepass.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.enelson.sopbattlepass.SopBattlePass;
import org.bukkit.OfflinePlayer;

public final class SopBattlePassExpansion extends PlaceholderExpansion {

    private final SopBattlePass plugin;
    private final BattlePassPlaceholders placeholders;

    public SopBattlePassExpansion(SopBattlePass plugin, BattlePassPlaceholders placeholders) {
        this.plugin = plugin;
        this.placeholders = placeholders;
    }

    @Override
    public String getIdentifier() {
        return "sopbattlepass";
    }

    @Override
    public String getAuthor() {
        return "E_NeLsOn";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        return placeholders.onRequest(player, params);
    }
}
