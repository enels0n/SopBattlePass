package net.enelson.sopbattlepass.reward;

import java.util.Collections;
import java.util.List;

public final class RewardView {

    private final LevelRewardDefinition definition;
    private final boolean reachedLevel;
    private final boolean premiumAvailable;
    private final boolean claimable;
    private final int usedClaims;
    private final List<String> claimedServerIds;

    public RewardView(LevelRewardDefinition definition,
                      boolean reachedLevel,
                      boolean premiumAvailable,
                      boolean claimable,
                      int usedClaims,
                      List<String> claimedServerIds) {
        this.definition = definition;
        this.reachedLevel = reachedLevel;
        this.premiumAvailable = premiumAvailable;
        this.claimable = claimable;
        this.usedClaims = usedClaims;
        this.claimedServerIds = Collections.unmodifiableList(claimedServerIds);
    }

    public LevelRewardDefinition getDefinition() {
        return definition;
    }

    public boolean isReachedLevel() {
        return reachedLevel;
    }

    public boolean isPremiumAvailable() {
        return premiumAvailable;
    }

    public boolean isClaimable() {
        return claimable;
    }

    public int getUsedClaims() {
        return usedClaims;
    }

    public List<String> getClaimedServerIds() {
        return claimedServerIds;
    }
}
