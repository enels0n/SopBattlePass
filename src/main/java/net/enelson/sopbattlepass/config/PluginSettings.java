package net.enelson.sopbattlepass.config;

import net.enelson.sopbattlepass.SopBattlePass;
import net.enelson.sopbattlepass.network.ServerIdentity;
import org.bukkit.configuration.file.FileConfiguration;

public final class PluginSettings {

    private final StorageSettings storage;
    private final ServerIdentity serverIdentity;
    private final String activeSeasonId;
    private final String timezone;
    private final String premiumPermissionNode;
    private final boolean premiumPermissionEnabled;
    private final boolean premiumSeasonFlagEnabled;
    private final double globalXpMultiplier;
    private final boolean allowManualReroll;

    private PluginSettings(StorageSettings storage,
                           ServerIdentity serverIdentity,
                           String activeSeasonId,
                           String timezone,
                           String premiumPermissionNode,
                           boolean premiumPermissionEnabled,
                           boolean premiumSeasonFlagEnabled,
                           double globalXpMultiplier,
                           boolean allowManualReroll) {
        this.storage = storage;
        this.serverIdentity = serverIdentity;
        this.activeSeasonId = activeSeasonId;
        this.timezone = timezone;
        this.premiumPermissionNode = premiumPermissionNode;
        this.premiumPermissionEnabled = premiumPermissionEnabled;
        this.premiumSeasonFlagEnabled = premiumSeasonFlagEnabled;
        this.globalXpMultiplier = globalXpMultiplier;
        this.allowManualReroll = allowManualReroll;
    }

    public static PluginSettings load(SopBattlePass plugin) {
        FileConfiguration config = plugin.getConfig();
        StorageSettings storage = StorageSettings.fromConfig(plugin);
        ServerIdentity serverIdentity = new ServerIdentity(
                config.getBoolean("network.enabled", false),
                config.getString("network.server-id", "default"),
                config.getString("network.server-group", "default")
        );
        return new PluginSettings(
                storage,
                serverIdentity,
                config.getString("season.active-id", "season-1"),
                config.getString("season.timezone", "UTC"),
                config.getString("premium.permission-node", "sopbattlepass.premium"),
                config.getBoolean("premium.permission-enabled", true),
                config.getBoolean("premium.season-flag-enabled", true),
                config.getDouble("boosters.global-multiplier", 1.0D),
                config.getBoolean("missions.admin.allow-manual-reroll", true)
        );
    }

    public StorageSettings getStorage() {
        return storage;
    }

    public ServerIdentity getServerIdentity() {
        return serverIdentity;
    }

    public String getActiveSeasonId() {
        return activeSeasonId;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getPremiumPermissionNode() {
        return premiumPermissionNode;
    }

    public boolean isPremiumPermissionEnabled() {
        return premiumPermissionEnabled;
    }

    public boolean isPremiumSeasonFlagEnabled() {
        return premiumSeasonFlagEnabled;
    }

    public double getGlobalXpMultiplier() {
        return globalXpMultiplier;
    }

    public boolean isAllowManualReroll() {
        return allowManualReroll;
    }
}
