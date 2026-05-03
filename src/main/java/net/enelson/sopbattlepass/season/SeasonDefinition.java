package net.enelson.sopbattlepass.season;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class SeasonDefinition {

    private final String id;
    private final String name;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final Map<Integer, Integer> levelRequiredTotalXp;

    public SeasonDefinition(String id, String name, LocalDateTime startsAt, LocalDateTime endsAt, Map<Integer, Integer> levelRequiredTotalXp) {
        this.id = id;
        this.name = name;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.levelRequiredTotalXp = Collections.unmodifiableMap(new TreeMap<Integer, Integer>(levelRequiredTotalXp));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public Map<Integer, Integer> getLevelRequiredTotalXp() {
        return levelRequiredTotalXp;
    }

    public int resolveLevelForXp(int xp) {
        int level = 1;
        for (Map.Entry<Integer, Integer> entry : levelRequiredTotalXp.entrySet()) {
            if (xp >= entry.getValue().intValue()) {
                level = entry.getKey().intValue();
            }
        }
        return level;
    }
}
