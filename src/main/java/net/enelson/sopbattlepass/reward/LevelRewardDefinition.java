package net.enelson.sopbattlepass.reward;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class LevelRewardDefinition {

    private final int level;
    private final RewardTrack track;
    private final RewardBundle defaultBundle;
    private final Map<String, RewardBundle> serverGroupBundles;
    private final List<String> eligibleServerIds;
    private final int claimLimit;

    public LevelRewardDefinition(int level,
                                 RewardTrack track,
                                 RewardBundle defaultBundle,
                                 Map<String, RewardBundle> serverGroupBundles,
                                 List<String> eligibleServerIds,
                                 int claimLimit) {
        this.level = level;
        this.track = track;
        this.defaultBundle = defaultBundle;
        this.serverGroupBundles = Collections.unmodifiableMap(serverGroupBundles);
        this.eligibleServerIds = Collections.unmodifiableList(eligibleServerIds);
        this.claimLimit = claimLimit;
    }

    public int getLevel() {
        return level;
    }

    public RewardTrack getTrack() {
        return track;
    }

    public RewardBundle getDefaultBundle() {
        return defaultBundle;
    }

    public List<String> getEligibleServerIds() {
        return eligibleServerIds;
    }

    public int getClaimLimit() {
        return claimLimit;
    }

    public RewardBundle resolveForServerGroup(String serverGroup) {
        RewardBundle override = serverGroupBundles.get(serverGroup);
        return override != null ? override : defaultBundle;
    }
}
