package net.enelson.sopbattlepass.config;

import net.enelson.sopbattlepass.SopBattlePass;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public final class StorageSettings {

    public enum Type {
        SQLITE,
        MYSQL
    }

    private final Type type;
    private final String tablePrefix;
    private final File sqliteFile;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String poolName;
    private final int maximumPoolSize;
    private final int minimumIdle;
    private final long connectionTimeout;
    private final long idleTimeout;
    private final long maxLifetime;

    private StorageSettings(Type type,
                            String tablePrefix,
                            File sqliteFile,
                            String host,
                            int port,
                            String database,
                            String username,
                            String password,
                            String poolName,
                            int maximumPoolSize,
                            int minimumIdle,
                            long connectionTimeout,
                            long idleTimeout,
                            long maxLifetime) {
        this.type = type;
        this.tablePrefix = tablePrefix;
        this.sqliteFile = sqliteFile;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.poolName = poolName;
        this.maximumPoolSize = maximumPoolSize;
        this.minimumIdle = minimumIdle;
        this.connectionTimeout = connectionTimeout;
        this.idleTimeout = idleTimeout;
        this.maxLifetime = maxLifetime;
    }

    public static StorageSettings fromConfig(SopBattlePass plugin) {
        FileConfiguration config = plugin.getConfig();
        String configuredType = config.getString("database.type", "sqlite");
        Type type = "mysql".equalsIgnoreCase(configuredType) ? Type.MYSQL : Type.SQLITE;
        return new StorageSettings(
                type,
                config.getString("database.table-prefix", "sopbattlepass_"),
                new File(plugin.getDataFolder(), config.getString("database.sqlite-file", "battlepass.db")),
                config.getString("database.host", "127.0.0.1"),
                config.getInt("database.port", 3306),
                config.getString("database.database", "minecraft"),
                config.getString("database.username", "root"),
                config.getString("database.password", ""),
                config.getString("database.pool-name", "SopBattlePass"),
                config.getInt("database.maximum-pool-size", 10),
                config.getInt("database.minimum-idle", 2),
                config.getLong("database.connection-timeout", 30000L),
                config.getLong("database.idle-timeout", 600000L),
                config.getLong("database.max-lifetime", 1800000L)
        );
    }

    public Type getType() {
        return type;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public File getSqliteFile() {
        return sqliteFile;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPoolName() {
        return poolName;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public int getMinimumIdle() {
        return minimumIdle;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }
}
