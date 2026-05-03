package net.enelson.sopbattlepass.reward;

import net.enelson.sopbattlepass.SopBattlePass;
import net.enelson.sopbattlepass.config.PluginSettings;
import net.enelson.sopbattlepass.player.PlayerBattlePassProgress;
import net.enelson.sopbattlepass.player.PlayerProgressService;
import net.enelson.sopbattlepass.storage.BattlePassRepository;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RewardService {

    private final SopBattlePass plugin;
    private final BattlePassRepository repository;
    private final PlayerProgressService playerProgressService;
    private final PluginSettings settings;
    private final Map<Integer, Map<RewardTrack, LevelRewardDefinition>> rewardDefinitions = new LinkedHashMap<Integer, Map<RewardTrack, LevelRewardDefinition>>();

    public RewardService(SopBattlePass plugin,
                         BattlePassRepository repository,
                         PlayerProgressService playerProgressService,
                         PluginSettings settings) {
        this.plugin = plugin;
        this.repository = repository;
        this.playerProgressService = playerProgressService;
        this.settings = settings;
        reload();
    }

    public void reload() {
        rewardDefinitions.clear();
        File file = new File(plugin.getDataFolder(), "seasons/" + settings.getActiveSeasonId() + ".yml");
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection rewardsSection = configuration.getConfigurationSection("rewards");
        if (rewardsSection == null) {
            return;
        }
        for (String levelKey : rewardsSection.getKeys(false)) {
            ConfigurationSection levelSection = rewardsSection.getConfigurationSection(levelKey);
            if (levelSection == null) {
                continue;
            }
            int level = Integer.parseInt(levelKey);
            Map<RewardTrack, LevelRewardDefinition> tracks = new LinkedHashMap<RewardTrack, LevelRewardDefinition>();
            LevelRewardDefinition free = loadTrack(level, RewardTrack.FREE, levelSection.getConfigurationSection("free"));
            LevelRewardDefinition premium = loadTrack(level, RewardTrack.PREMIUM, levelSection.getConfigurationSection("premium"));
            if (free != null) {
                tracks.put(RewardTrack.FREE, free);
            }
            if (premium != null) {
                tracks.put(RewardTrack.PREMIUM, premium);
            }
            rewardDefinitions.put(Integer.valueOf(level), tracks);
        }
    }

    public List<RewardView> getRewardViews(OfflinePlayer player) throws SQLException {
        PlayerBattlePassProgress progress = playerProgressService.getProgress(player);
        List<RewardView> result = new ArrayList<RewardView>();
        for (Map<RewardTrack, LevelRewardDefinition> byTrack : rewardDefinitions.values()) {
            for (LevelRewardDefinition definition : byTrack.values()) {
                List<RewardClaimRecord> claims = repository.findRewardClaims(player.getUniqueId().toString(), progress.getSeasonId(), definition.getLevel(), definition.getTrack());
                boolean reached = progress.getLevel() >= definition.getLevel();
                boolean premiumAvailable = definition.getTrack() != RewardTrack.PREMIUM || progress.isPremiumActive();
                boolean claimable = reached && premiumAvailable && canClaim(definition, claims, settings.getServerIdentity().getServerId());
                result.add(new RewardView(definition, reached, premiumAvailable, claimable, claims.size(), extractServerIds(claims)));
            }
        }
        Collections.sort(result, (left, right) -> {
            int level = Integer.compare(left.getDefinition().getLevel(), right.getDefinition().getLevel());
            if (level != 0) {
                return level;
            }
            return left.getDefinition().getTrack().compareTo(right.getDefinition().getTrack());
        });
        return result;
    }

    public RewardClaimResult claim(Player player, int level, RewardTrack track) throws SQLException {
        Map<RewardTrack, LevelRewardDefinition> byTrack = rewardDefinitions.get(Integer.valueOf(level));
        if (byTrack == null) {
            return new RewardClaimResult(false, "&cReward level not found.");
        }
        LevelRewardDefinition definition = byTrack.get(track);
        if (definition == null) {
            return new RewardClaimResult(false, "&cReward track not configured.");
        }
        PlayerBattlePassProgress progress = playerProgressService.getProgress(player);
        if (progress.getLevel() < level) {
            return new RewardClaimResult(false, "&cYou have not reached this level yet.");
        }
        if (track == RewardTrack.PREMIUM && !progress.isPremiumActive()) {
            return new RewardClaimResult(false, "&cPremium is required for this reward.");
        }

        String currentServerId = settings.getServerIdentity().getServerId();
        List<RewardClaimRecord> claims = repository.findRewardClaims(player.getUniqueId().toString(), progress.getSeasonId(), level, track);
        if (!canClaim(definition, claims, currentServerId)) {
            return new RewardClaimResult(false, "&cYou cannot claim this reward on this backend.");
        }

        RewardBundle bundle = definition.resolveForServerGroup(settings.getServerIdentity().getServerGroup());
        if (bundle == null || bundle.getCommands().isEmpty()) {
            return new RewardClaimResult(false, "&cNo commands configured for this reward.");
        }
        for (String rawCommand : bundle.getCommands()) {
            String command = rawCommand
                    .replace("{player}", player.getName())
                    .replace("{server_id}", settings.getServerIdentity().getServerId())
                    .replace("{server_group}", settings.getServerIdentity().getServerGroup())
                    .replace("{level}", Integer.toString(level))
                    .replace("{track}", track.name().toLowerCase(Locale.ROOT));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        repository.saveRewardClaim(new RewardClaimRecord(
                player.getUniqueId().toString(),
                progress.getSeasonId(),
                level,
                track,
                currentServerId,
                System.currentTimeMillis() / 1000L
        ));
        return new RewardClaimResult(true, "&aReward claimed.");
    }

    private LevelRewardDefinition loadTrack(int level, RewardTrack track, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        RewardBundle defaultBundle = new RewardBundle(new ArrayList<String>(section.getStringList("commands")));
        Map<String, RewardBundle> overrides = new LinkedHashMap<String, RewardBundle>();
        ConfigurationSection groups = section.getConfigurationSection("server-groups");
        if (groups != null) {
            for (String groupKey : groups.getKeys(false)) {
                ConfigurationSection groupSection = groups.getConfigurationSection(groupKey);
                if (groupSection == null) {
                    continue;
                }
                overrides.put(groupKey, new RewardBundle(new ArrayList<String>(groupSection.getStringList("commands"))));
            }
        }

        List<String> eligible = new ArrayList<String>(section.getStringList("eligible-server-ids"));
        if (eligible.isEmpty()) {
            eligible.addAll(plugin.getConfig().getStringList("rewards.claim.default-eligible-server-ids"));
        }
        int claimLimit = section.getInt("claim-limit", plugin.getConfig().getInt("rewards.claim.default-claim-limit", 1));
        return new LevelRewardDefinition(level, track, defaultBundle, overrides, eligible, claimLimit);
    }

    private boolean canClaim(LevelRewardDefinition definition, List<RewardClaimRecord> claims, String currentServerId) {
        if (!definition.getEligibleServerIds().isEmpty() && !definition.getEligibleServerIds().contains(currentServerId)) {
            return false;
        }
        for (RewardClaimRecord claim : claims) {
            if (claim.getServerId().equalsIgnoreCase(currentServerId)) {
                return false;
            }
        }
        return definition.getClaimLimit() <= 0 || claims.size() < definition.getClaimLimit();
    }

    private List<String> extractServerIds(List<RewardClaimRecord> claims) {
        List<String> result = new ArrayList<String>();
        for (RewardClaimRecord claim : claims) {
            result.add(claim.getServerId());
        }
        return result;
    }
}
