package net.enelson.sopbattlepass.player;

public final class PlayerBattlePassProgress {

    private final String playerUuid;
    private final String seasonId;
    private final int xp;
    private final int level;
    private final boolean premiumActive;

    public PlayerBattlePassProgress(String playerUuid, String seasonId, int xp, int level, boolean premiumActive) {
        this.playerUuid = playerUuid;
        this.seasonId = seasonId;
        this.xp = xp;
        this.level = level;
        this.premiumActive = premiumActive;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public int getXp() {
        return xp;
    }

    public int getLevel() {
        return level;
    }

    public boolean isPremiumActive() {
        return premiumActive;
    }
}
