package net.enelson.sopbattlepass.mission;

public final class PlayerMissionProgress {

    private final String playerUuid;
    private final String seasonId;
    private final String missionKey;
    private final int progress;
    private final boolean completed;
    private final long completedAtEpochSeconds;

    public PlayerMissionProgress(String playerUuid, String seasonId, String missionKey, int progress, boolean completed, long completedAtEpochSeconds) {
        this.playerUuid = playerUuid;
        this.seasonId = seasonId;
        this.missionKey = missionKey;
        this.progress = progress;
        this.completed = completed;
        this.completedAtEpochSeconds = completedAtEpochSeconds;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public String getMissionKey() {
        return missionKey;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public long getCompletedAtEpochSeconds() {
        return completedAtEpochSeconds;
    }
}
