package net.enelson.sopbattlepass.reward;

public final class RewardClaimRecord {

    private final String playerUuid;
    private final String seasonId;
    private final int level;
    private final RewardTrack track;
    private final String serverId;
    private final long claimedAt;

    public RewardClaimRecord(String playerUuid, String seasonId, int level, RewardTrack track, String serverId, long claimedAt) {
        this.playerUuid = playerUuid;
        this.seasonId = seasonId;
        this.level = level;
        this.track = track;
        this.serverId = serverId;
        this.claimedAt = claimedAt;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public int getLevel() {
        return level;
    }

    public RewardTrack getTrack() {
        return track;
    }

    public String getServerId() {
        return serverId;
    }

    public long getClaimedAt() {
        return claimedAt;
    }
}
