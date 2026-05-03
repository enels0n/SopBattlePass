package net.enelson.sopbattlepass.storage;

import net.enelson.sopbattlepass.SopBattlePass;
import net.enelson.sopbattlepass.config.PluginSettings;
import net.enelson.sopbattlepass.config.StorageSettings;
import net.enelson.sopli.lib.database.DatabaseConfig;
import net.enelson.sopli.lib.database.SopDatabase;

public final class DatabaseFactory {

    private final SopBattlePass plugin;
    private final PluginSettings settings;

    public DatabaseFactory(SopBattlePass plugin, PluginSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public SopDatabase create() {
        StorageSettings storage = settings.getStorage();
        DatabaseConfig config;
        if (storage.getType() == StorageSettings.Type.MYSQL) {
            config = DatabaseConfig.mysql(storage.getHost(), storage.getPort(), storage.getDatabase())
                    .credentials(storage.getUsername(), storage.getPassword())
                    .poolName(storage.getPoolName() + "-" + settings.getServerIdentity().getServerId())
                    .maximumPoolSize(storage.getMaximumPoolSize())
                    .minimumIdle(storage.getMinimumIdle())
                    .connectionTimeout(storage.getConnectionTimeout())
                    .idleTimeout(storage.getIdleTimeout())
                    .maxLifetime(storage.getMaxLifetime())
                    .build();
        } else {
            String path = storage.getSqliteFile().getAbsolutePath().replace("\\", "/");
            config = DatabaseConfig.builder("jdbc:sqlite:" + path)
                    .poolName(storage.getPoolName() + "-sqlite")
                    .maximumPoolSize(1)
                    .minimumIdle(1)
                    .connectionTimeout(storage.getConnectionTimeout())
                    .idleTimeout(storage.getIdleTimeout())
                    .maxLifetime(storage.getMaxLifetime())
                    .build();
        }
        return net.enelson.sopli.lib.SopLib.getInstance().getDatabaseService().createDatabase(config);
    }
}
