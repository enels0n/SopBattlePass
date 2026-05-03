package net.enelson.sopbattlepass.storage;

import net.enelson.sopbattlepass.config.PluginSettings;
import net.enelson.sopbattlepass.mission.PlayerMissionProgress;
import net.enelson.sopbattlepass.player.PlayerBattlePassProgress;
import net.enelson.sopbattlepass.reward.RewardClaimRecord;
import net.enelson.sopbattlepass.reward.RewardTrack;
import net.enelson.sopbattlepass.season.SeasonDefinition;
import net.enelson.sopli.lib.database.SopDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SqlBattlePassRepository implements BattlePassRepository {

    private final SopDatabase database;
    private final String tablePrefix;
    private final SeasonDefinition seasonDefinition;

    public SqlBattlePassRepository(SopDatabase database, PluginSettings settings, SeasonDefinition seasonDefinition) {
        this.database = database;
        this.tablePrefix = settings.getStorage().getTablePrefix();
        this.seasonDefinition = seasonDefinition;
    }

    @Override
    public void initialize() throws SQLException {
        database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "player_progress ("
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "season_id VARCHAR(64) NOT NULL,"
                + "xp INTEGER NOT NULL,"
                + "level INTEGER NOT NULL,"
                + "premium_enabled INTEGER NOT NULL,"
                + "updated_at BIGINT NOT NULL,"
                + "PRIMARY KEY (player_uuid, season_id)"
                + ")");
        database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "rerolls ("
                + "mission_type VARCHAR(16) NOT NULL,"
                + "season_id VARCHAR(64) NOT NULL,"
                + "updated_at BIGINT NOT NULL,"
                + "PRIMARY KEY (mission_type, season_id)"
                + ")");
        database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "mission_progress ("
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "season_id VARCHAR(64) NOT NULL,"
                + "mission_key VARCHAR(255) NOT NULL,"
                + "progress INTEGER NOT NULL,"
                + "completed INTEGER NOT NULL,"
                + "completed_at BIGINT NOT NULL,"
                + "updated_at BIGINT NOT NULL,"
                + "PRIMARY KEY (player_uuid, season_id, mission_key)"
                + ")");
        database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "reward_claims ("
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "season_id VARCHAR(64) NOT NULL,"
                + "level INTEGER NOT NULL,"
                + "track VARCHAR(16) NOT NULL,"
                + "server_id VARCHAR(128) NOT NULL,"
                + "claimed_at BIGINT NOT NULL,"
                + "PRIMARY KEY (player_uuid, season_id, level, track, server_id)"
                + ")");
    }

    @Override
    public PlayerBattlePassProgress findProgress(final String playerUuid, final String seasonId) throws SQLException {
        return database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement("SELECT xp, level, premium_enabled FROM " + tablePrefix + "player_progress WHERE player_uuid = ? AND season_id = ?");
                statement.setString(1, playerUuid);
                statement.setString(2, seasonId);
                resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    return null;
                }
                return new PlayerBattlePassProgress(
                        playerUuid,
                        seasonId,
                        resultSet.getInt("xp"),
                        resultSet.getInt("level"),
                        resultSet.getInt("premium_enabled") == 1
                );
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public void saveProgress(final PlayerBattlePassProgress progress) throws SQLException {
        database.transaction(connection -> {
            upsertProgress(connection, progress);
            return null;
        });
    }

    @Override
    public Map<String, PlayerMissionProgress> findMissionProgress(final String playerUuid, final String seasonId) throws SQLException {
        return database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            Map<String, PlayerMissionProgress> result = new HashMap<String, PlayerMissionProgress>();
            try {
                statement = connection.prepareStatement("SELECT mission_key, progress, completed, completed_at FROM " + tablePrefix + "mission_progress WHERE player_uuid = ? AND season_id = ?");
                statement.setString(1, playerUuid);
                statement.setString(2, seasonId);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    PlayerMissionProgress progress = new PlayerMissionProgress(
                            playerUuid,
                            seasonId,
                            resultSet.getString("mission_key"),
                            resultSet.getInt("progress"),
                            resultSet.getInt("completed") == 1,
                            resultSet.getLong("completed_at")
                    );
                    result.put(progress.getMissionKey(), progress);
                }
                return result;
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public void saveMissionProgress(final PlayerMissionProgress progress) throws SQLException {
        database.transaction(connection -> {
            upsertMissionProgress(connection, progress);
            return null;
        });
    }

    @Override
    public List<RewardClaimRecord> findRewardClaims(final String playerUuid, final String seasonId, final int level, final RewardTrack track) throws SQLException {
        return database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            List<RewardClaimRecord> result = new ArrayList<RewardClaimRecord>();
            try {
                statement = connection.prepareStatement("SELECT server_id, claimed_at FROM " + tablePrefix + "reward_claims WHERE player_uuid = ? AND season_id = ? AND level = ? AND track = ?");
                statement.setString(1, playerUuid);
                statement.setString(2, seasonId);
                statement.setInt(3, level);
                statement.setString(4, track.name());
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    result.add(new RewardClaimRecord(
                            playerUuid,
                            seasonId,
                            level,
                            track,
                            resultSet.getString("server_id"),
                            resultSet.getLong("claimed_at")
                    ));
                }
                return result;
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public void saveRewardClaim(final RewardClaimRecord rewardClaimRecord) throws SQLException {
        database.withConnection(connection -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement("INSERT INTO " + tablePrefix + "reward_claims (player_uuid, season_id, level, track, server_id, claimed_at) VALUES (?, ?, ?, ?, ?, ?)");
                statement.setString(1, rewardClaimRecord.getPlayerUuid());
                statement.setString(2, rewardClaimRecord.getSeasonId());
                statement.setInt(3, rewardClaimRecord.getLevel());
                statement.setString(4, rewardClaimRecord.getTrack().name());
                statement.setString(5, rewardClaimRecord.getServerId());
                statement.setLong(6, rewardClaimRecord.getClaimedAt());
                statement.executeUpdate();
            } finally {
                closeQuietly(statement);
            }
        });
    }

    @Override
    public void touchMissionReroll(String missionType, long epochSeconds) throws SQLException {
        database.transaction(connection -> {
            PreparedStatement delete = null;
            PreparedStatement insert = null;
            try {
                delete = connection.prepareStatement("DELETE FROM " + tablePrefix + "rerolls WHERE mission_type = ? AND season_id = ?");
                delete.setString(1, missionType);
                delete.setString(2, seasonDefinition.getId());
                delete.executeUpdate();

                insert = connection.prepareStatement("INSERT INTO " + tablePrefix + "rerolls (mission_type, season_id, updated_at) VALUES (?, ?, ?)");
                insert.setString(1, missionType);
                insert.setString(2, seasonDefinition.getId());
                insert.setLong(3, epochSeconds);
                insert.executeUpdate();
                return null;
            } finally {
                closeQuietly(delete);
                closeQuietly(insert);
            }
        });
    }

    @Override
    public long findMissionReroll(final String missionType) throws SQLException {
        return database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement("SELECT updated_at FROM " + tablePrefix + "rerolls WHERE mission_type = ? AND season_id = ?");
                statement.setString(1, missionType);
                statement.setString(2, seasonDefinition.getId());
                resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    return Long.valueOf(0L);
                }
                return Long.valueOf(resultSet.getLong("updated_at"));
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        }).longValue();
    }

    private void upsertProgress(Connection connection, PlayerBattlePassProgress progress) throws SQLException {
        PreparedStatement delete = null;
        PreparedStatement insert = null;
        try {
            delete = connection.prepareStatement("DELETE FROM " + tablePrefix + "player_progress WHERE player_uuid = ? AND season_id = ?");
            delete.setString(1, progress.getPlayerUuid());
            delete.setString(2, progress.getSeasonId());
            delete.executeUpdate();

            insert = connection.prepareStatement("INSERT INTO " + tablePrefix + "player_progress (player_uuid, season_id, xp, level, premium_enabled, updated_at) VALUES (?, ?, ?, ?, ?, ?)");
            insert.setString(1, progress.getPlayerUuid());
            insert.setString(2, progress.getSeasonId());
            insert.setInt(3, progress.getXp());
            insert.setInt(4, progress.getLevel());
            insert.setInt(5, progress.isPremiumActive() ? 1 : 0);
            insert.setLong(6, System.currentTimeMillis() / 1000L);
            insert.executeUpdate();
        } finally {
            closeQuietly(delete);
            closeQuietly(insert);
        }
    }

    private void upsertMissionProgress(Connection connection, PlayerMissionProgress progress) throws SQLException {
        PreparedStatement delete = null;
        PreparedStatement insert = null;
        try {
            delete = connection.prepareStatement("DELETE FROM " + tablePrefix + "mission_progress WHERE player_uuid = ? AND season_id = ? AND mission_key = ?");
            delete.setString(1, progress.getPlayerUuid());
            delete.setString(2, progress.getSeasonId());
            delete.setString(3, progress.getMissionKey());
            delete.executeUpdate();

            insert = connection.prepareStatement("INSERT INTO " + tablePrefix + "mission_progress (player_uuid, season_id, mission_key, progress, completed, completed_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)");
            insert.setString(1, progress.getPlayerUuid());
            insert.setString(2, progress.getSeasonId());
            insert.setString(3, progress.getMissionKey());
            insert.setInt(4, progress.getProgress());
            insert.setInt(5, progress.isCompleted() ? 1 : 0);
            insert.setLong(6, progress.getCompletedAtEpochSeconds());
            insert.setLong(7, System.currentTimeMillis() / 1000L);
            insert.executeUpdate();
        } finally {
            closeQuietly(delete);
            closeQuietly(insert);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
