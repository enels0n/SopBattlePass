package net.enelson.sopbattlepass.storage;

import net.enelson.sopbattlepass.mission.PlayerMissionProgress;
import net.enelson.sopbattlepass.player.PlayerBattlePassProgress;
import net.enelson.sopbattlepass.reward.RewardClaimRecord;
import net.enelson.sopbattlepass.reward.RewardTrack;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface BattlePassRepository {

    void initialize() throws SQLException;

    PlayerBattlePassProgress findProgress(String playerUuid, String seasonId) throws SQLException;

    void saveProgress(PlayerBattlePassProgress progress) throws SQLException;

    Map<String, PlayerMissionProgress> findMissionProgress(String playerUuid, String seasonId) throws SQLException;

    void saveMissionProgress(PlayerMissionProgress progress) throws SQLException;

    List<RewardClaimRecord> findRewardClaims(String playerUuid, String seasonId, int level, RewardTrack track) throws SQLException;

    void saveRewardClaim(RewardClaimRecord rewardClaimRecord) throws SQLException;

    void touchMissionReroll(String missionType, long epochSeconds) throws SQLException;

    long findMissionReroll(String missionType) throws SQLException;
}
