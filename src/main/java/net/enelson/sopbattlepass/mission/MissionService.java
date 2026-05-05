package net.enelson.sopbattlepass.mission;

import net.enelson.sopbattlepass.SopBattlePass;
import net.enelson.sopbattlepass.config.PluginSettings;
import net.enelson.sopbattlepass.gui.MenuItemSpec;
import net.enelson.sopbattlepass.player.PlayerProgressService;
import net.enelson.sopbattlepass.season.SeasonDefinition;
import net.enelson.sopbattlepass.season.SeasonService;
import net.enelson.sopbattlepass.storage.BattlePassRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class MissionService {

    private final SopBattlePass plugin;
    private final BattlePassRepository repository;
    private final SeasonService seasonService;
    private final PlayerProgressService playerProgressService;
    private final PluginSettings settings;
    private final ZoneId zoneId;
    private final Map<MissionType, Map<String, MissionTemplate>> templateMaps = new HashMap<MissionType, Map<String, MissionTemplate>>();
    private final Map<Integer, List<String>> weeklyFixedMissionIds = new HashMap<Integer, List<String>>();

    public MissionService(SopBattlePass plugin,
                          BattlePassRepository repository,
                          SeasonService seasonService,
                          PlayerProgressService playerProgressService,
                          PluginSettings settings) {
        this.plugin = plugin;
        this.repository = repository;
        this.seasonService = seasonService;
        this.playerProgressService = playerProgressService;
        this.settings = settings;
        this.zoneId = ZoneId.of(settings.getTimezone());
        reload();
    }

    public void reload() {
        templateMaps.clear();
        weeklyFixedMissionIds.clear();
        templateMaps.put(MissionType.DAILY, loadTemplates(new File(plugin.getDataFolder(), "missions/daily.yml")));
        YamlConfiguration weeklyConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "missions/weekly.yml"));
        templateMaps.put(MissionType.WEEKLY, loadTemplates(weeklyConfig));
        templateMaps.put(MissionType.GLOBAL, loadTemplates(new File(plugin.getDataFolder(), "missions/global.yml")));

        ConfigurationSection weeks = weeklyConfig.getConfigurationSection("weeks");
        if (weeks != null) {
            for (String key : weeks.getKeys(false)) {
                ConfigurationSection weekSection = weeks.getConfigurationSection(key);
                if (weekSection == null) {
                    continue;
                }
                weeklyFixedMissionIds.put(Integer.valueOf(Integer.parseInt(key)), new ArrayList<String>(weekSection.getStringList("missions")));
            }
        }
    }

    public List<MissionView> getMissionViews(OfflinePlayer player, MissionType type) throws SQLException {
        Map<String, PlayerMissionProgress> progressMap = repository.findMissionProgress(player.getUniqueId().toString(), seasonService.getActiveSeason().getId());
        List<ActiveMission> activeMissions = getActiveMissions(type);
        List<MissionView> result = new ArrayList<MissionView>();
        for (ActiveMission activeMission : activeMissions) {
            PlayerMissionProgress progress = progressMap.get(activeMission.getKey());
            int progressValue = progress == null ? 0 : progress.getProgress();
            boolean completed = progress != null && progress.isCompleted();
            List<String> resolved = new ArrayList<String>();
            for (String line : activeMission.getTemplate().getDescription()) {
                resolved.add(line
                        .replace("{required}", Integer.toString(activeMission.getTemplate().getRequired()))
                        .replace("{progress}", Integer.toString(progressValue))
                        .replace("{xp}", Integer.toString(activeMission.getTemplate().getXp()))
                        .replace("{week}", Integer.toString(activeMission.getWeekNumber()))
                        .replace("{target}", formatTarget(activeMission.getTemplate().getTarget())));
            }
            result.add(new MissionView(activeMission, progressValue, completed, resolved));
        }
        return result;
    }

    public void recordSimpleProgress(Player player, MissionTriggerType triggerType, String targetKey, int amount) {
        try {
            SeasonDefinition season = seasonService.getActiveSeason();
            Map<String, PlayerMissionProgress> progressMap = repository.findMissionProgress(player.getUniqueId().toString(), season.getId());
            String serverGroup = settings.getServerIdentity().getServerGroup();
            for (MissionType type : new MissionType[]{MissionType.DAILY, MissionType.WEEKLY, MissionType.GLOBAL}) {
                for (ActiveMission mission : getActiveMissions(type)) {
                    if (!serverGroup.equalsIgnoreCase(mission.getTemplate().getServerGroup())) {
                        continue;
                    }
                    if (mission.getTemplate().getTriggerType() != triggerType) {
                        continue;
                    }
                    if (!matchesTarget(mission.getTemplate(), targetKey)) {
                        continue;
                    }
                    if (!mission.getTemplate().getConditions().test(this, player)) {
                        continue;
                    }
                    applyMissionProgress(player, mission, amount, progressMap);
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Mission progress update failed: " + exception.getMessage());
        }
    }

    public void recordCustomProgress(Player player, String customTargetKey, int amount) {
        recordSimpleProgress(player, MissionTriggerType.CUSTOM, customTargetKey, amount);
    }

    public void reroll(MissionType type) throws SQLException {
        repository.touchMissionReroll(type.name().toLowerCase(Locale.ROOT), System.currentTimeMillis() / 1000L);
    }

    public List<ActiveMission> getActiveMissions(MissionType type) throws SQLException {
        if (!isCategoryEnabled(type)) {
            return Collections.emptyList();
        }
        if (type == MissionType.DAILY) {
            return resolveDailyMissions();
        }
        if (type == MissionType.WEEKLY) {
            return resolveWeeklyMissions();
        }
        return resolveGlobalMissions();
    }

    public boolean isCategoryEnabled(MissionType type) {
        if (type == MissionType.DAILY) {
            return plugin.getConfig().getBoolean("missions.daily.enabled", true);
        }
        if (type == MissionType.WEEKLY) {
            return plugin.getConfig().getBoolean("missions.weekly.enabled", true);
        }
        return plugin.getConfig().getBoolean("missions.global.enabled", true);
    }

    private void applyMissionProgress(Player player,
                                      ActiveMission mission,
                                      int amount,
                                      Map<String, PlayerMissionProgress> progressMap) throws SQLException {
        PlayerMissionProgress current = progressMap.get(mission.getKey());
        if (current != null && current.isCompleted()) {
            return;
        }

        int currentValue = current == null ? 0 : current.getProgress();
        int newValue = Math.min(mission.getTemplate().getRequired(), currentValue + amount);
        boolean completed = newValue >= mission.getTemplate().getRequired();
        long completedAt = completed ? (System.currentTimeMillis() / 1000L) : 0L;
        PlayerMissionProgress updated = new PlayerMissionProgress(
                player.getUniqueId().toString(),
                seasonService.getActiveSeason().getId(),
                mission.getKey(),
                newValue,
                completed,
                completedAt
        );
        repository.saveMissionProgress(updated);
        progressMap.put(updated.getMissionKey(), updated);

        if (completed) {
            playerProgressService.addXp(player, mission.getTemplate().getXp());
            player.sendMessage(plugin.getTextUtils().color(
                    plugin.getConfig().getString("messages.mission-completed", "{prefix}&aMission completed: &e{mission} &7(+{xp} xp)")
                            .replace("{prefix}", plugin.getConfig().getString("messages.prefix", ""))
                            .replace("{mission}", mission.getTemplate().getDisplayName())
                            .replace("{xp}", Integer.toString(mission.getTemplate().getXp()))
            ));
            if (!mission.getSharedSlotKeys().isEmpty()) {
                mirrorSharedSlotCompletion(player, mission, progressMap);
            }
            return;
        }

        sendMissionProgressMessage(player, mission, currentValue, newValue);
    }

    private void mirrorSharedSlotCompletion(Player player,
                                            ActiveMission sourceMission,
                                            Map<String, PlayerMissionProgress> progressMap) throws SQLException {
        for (String sharedMissionKey : sourceMission.getSharedSlotKeys()) {
            PlayerMissionProgress current = progressMap.get(sharedMissionKey);
            if (current != null && current.isCompleted()) {
                continue;
            }
            PlayerMissionProgress mirrored = new PlayerMissionProgress(
                    player.getUniqueId().toString(),
                    seasonService.getActiveSeason().getId(),
                    sharedMissionKey,
                    sourceMission.getTemplate().getRequired(),
                    true,
                    System.currentTimeMillis() / 1000L
            );
            repository.saveMissionProgress(mirrored);
            progressMap.put(sharedMissionKey, mirrored);
        }
    }

    private List<ActiveMission> resolveDailyMissions() throws SQLException {
        Map<String, MissionTemplate> templates = templateMaps.get(MissionType.DAILY);
        int amountPerGroup = plugin.getConfig().getInt("missions.daily.amount-per-group", 5);
        boolean allowRepeat = plugin.getConfig().getBoolean("missions.daily.allow-template-repeat", true);
        boolean sharedSlots = plugin.getConfig().getBoolean("missions.daily.shared-slot-progress", true);
        String periodKey = LocalDate.now(zoneId).toString();
        long rerollSeed = getOrCreateRotationSeed("daily");
        Map<String, List<MissionTemplate>> byGroup = groupByServerGroup(templates);
        List<ActiveMission> result = new ArrayList<ActiveMission>();
        for (Map.Entry<String, List<MissionTemplate>> entry : byGroup.entrySet()) {
            String serverGroup = entry.getKey();
            Map<Integer, MissionTemplate> forcedSlots = resolveForcedDailySlots(templates);
            List<MissionTemplate> selected = selectTemplates(removeForcedTemplates(entry.getValue(), forcedSlots), Math.max(0, amountPerGroup - forcedSlots.size()), allowRepeat, seedFor("daily", serverGroup, periodKey, rerollSeed));
            int selectedIndex = 0;
            for (int slot = 1; slot <= amountPerGroup; slot++) {
                MissionTemplate template = forcedSlots.get(Integer.valueOf(slot));
                if (template == null) {
                    if (selectedIndex >= selected.size()) {
                        break;
                    }
                    template = selected.get(selectedIndex++);
                }
                result.add(new ActiveMission(
                        MissionType.DAILY,
                        missionKey(MissionType.DAILY, periodKey, serverGroup, slot, template.getId()),
                        slot,
                        periodKey,
                        0,
                        template,
                        sharedSlots ? buildSharedSlotKeys(byGroup, MissionType.DAILY, periodKey, slot, template.getId(), serverGroup) : Collections.<String>emptyList()
                ));
            }
        }
        return sortMissions(result);
    }

    private List<ActiveMission> resolveWeeklyMissions() throws SQLException {
        Map<String, MissionTemplate> templates = templateMaps.get(MissionType.WEEKLY);
        boolean sharedSlots = plugin.getConfig().getBoolean("missions.weekly.shared-slot-progress", true);
        boolean fixed = "fixed".equalsIgnoreCase(plugin.getConfig().getString("missions.weekly.mode", "fixed"));
        boolean keepActive = plugin.getConfig().getBoolean("missions.weekly.keep-weekly-active", true);
        boolean allowRepeat = plugin.getConfig().getBoolean("missions.weekly.allow-template-repeat", false);
        int amountPerWeek = plugin.getConfig().getInt("missions.weekly.amount-per-week", 5);
        int currentWeek = currentSeasonWeek();
        int firstWeek = keepActive ? 1 : currentWeek;
        long rerollSeed = getOrCreateRotationSeed("weekly");
        Map<String, List<MissionTemplate>> byGroup = groupByServerGroup(templates);
        List<ActiveMission> result = new ArrayList<ActiveMission>();

        for (int week = firstWeek; week <= currentWeek; week++) {
            String periodKey = "week-" + week;
            if (fixed) {
                List<String> ids = weeklyFixedMissionIds.get(Integer.valueOf(week));
                if (ids == null) {
                    continue;
                }
                int slot = 1;
                for (String id : ids) {
                    MissionTemplate template = templates.get(id);
                    if (template == null) {
                        continue;
                    }
                    result.add(new ActiveMission(
                            MissionType.WEEKLY,
                            missionKey(MissionType.WEEKLY, periodKey, template.getServerGroup(), slot, template.getId()),
                            slot,
                            periodKey,
                            week,
                            template,
                            sharedSlots ? buildSharedSlotKeys(byGroup, MissionType.WEEKLY, periodKey, slot, template.getId(), template.getServerGroup()) : Collections.<String>emptyList()
                    ));
                    slot++;
                }
            } else {
                for (Map.Entry<String, List<MissionTemplate>> entry : byGroup.entrySet()) {
                    String serverGroup = entry.getKey();
                    List<MissionTemplate> selected = selectTemplates(entry.getValue(), amountPerWeek, allowRepeat, seedFor("weekly", serverGroup, periodKey, rerollSeed));
                    for (int index = 0; index < selected.size(); index++) {
                        int slot = index + 1;
                        MissionTemplate template = selected.get(index);
                        result.add(new ActiveMission(
                                MissionType.WEEKLY,
                                missionKey(MissionType.WEEKLY, periodKey, serverGroup, slot, template.getId()),
                                slot,
                                periodKey,
                                week,
                                template,
                                sharedSlots ? buildSharedSlotKeys(byGroup, MissionType.WEEKLY, periodKey, slot, template.getId(), serverGroup) : Collections.<String>emptyList()
                        ));
                    }
                }
            }
        }
        return sortMissions(result);
    }

    private List<ActiveMission> resolveGlobalMissions() {
        Map<String, MissionTemplate> templates = templateMaps.get(MissionType.GLOBAL);
        List<ActiveMission> result = new ArrayList<ActiveMission>();
        int slot = 1;
        for (MissionTemplate template : templates.values()) {
            result.add(new ActiveMission(
                    MissionType.GLOBAL,
                    missionKey(MissionType.GLOBAL, seasonService.getActiveSeason().getId(), template.getServerGroup(), slot, template.getId()),
                    slot,
                    seasonService.getActiveSeason().getId(),
                    0,
                    template,
                    Collections.<String>emptyList()
            ));
            slot++;
        }
        return sortMissions(result);
    }

    private List<MissionTemplate> selectTemplates(List<MissionTemplate> source, int count, boolean allowRepeat, long seed) {
        if (source.isEmpty() || count <= 0) {
            return Collections.emptyList();
        }
        List<MissionTemplate> working = new ArrayList<MissionTemplate>(source);
        Collections.sort(working, Comparator.comparing(MissionTemplate::getId));
        Random random = new Random(seed);
        List<MissionTemplate> selected = new ArrayList<MissionTemplate>();
        if (allowRepeat) {
            List<MissionTemplate> shuffled = new ArrayList<MissionTemplate>(working);
            Collections.shuffle(shuffled, random);
            while (selected.size() < count) {
                for (MissionTemplate template : shuffled) {
                    if (selected.size() >= count) {
                        break;
                    }
                    selected.add(template);
                }
                if (selected.size() < count) {
                    Collections.shuffle(shuffled, random);
                    avoidSameTail(selected, shuffled);
                }
            }
            return selected;
        }
        List<MissionTemplate> shuffled = new ArrayList<MissionTemplate>(working);
        Collections.shuffle(shuffled, random);
        int limit = Math.min(count, shuffled.size());
        for (int index = 0; index < limit; index++) {
            selected.add(shuffled.get(index));
        }
        return selected;
    }

    private void avoidSameTail(List<MissionTemplate> selected, List<MissionTemplate> nextBatch) {
        if (selected.isEmpty() || nextBatch.size() < 2) {
            return;
        }
        MissionTemplate lastSelected = selected.get(selected.size() - 1);
        if (!lastSelected.getId().equalsIgnoreCase(nextBatch.get(0).getId())) {
            return;
        }
        for (int index = 1; index < nextBatch.size(); index++) {
            if (!lastSelected.getId().equalsIgnoreCase(nextBatch.get(index).getId())) {
                MissionTemplate replacement = nextBatch.get(index);
                nextBatch.set(index, nextBatch.get(0));
                nextBatch.set(0, replacement);
                return;
            }
        }
    }

    private Map<String, List<MissionTemplate>> groupByServerGroup(Map<String, MissionTemplate> templates) {
        Map<String, List<MissionTemplate>> result = new LinkedHashMap<String, List<MissionTemplate>>();
        for (MissionTemplate template : templates.values()) {
            List<MissionTemplate> list = result.get(template.getServerGroup());
            if (list == null) {
                list = new ArrayList<MissionTemplate>();
                result.put(template.getServerGroup(), list);
            }
            list.add(template);
        }
        return result;
    }

    private long seedFor(String type, String serverGroup, String periodKey, long rerollSeed) {
        return (type + "|" + serverGroup + "|" + periodKey + "|" + rerollSeed).hashCode();
    }

    private long getOrCreateRotationSeed(String type) throws SQLException {
        long seed = repository.findMissionReroll(type);
        if (seed != 0L) {
            return seed;
        }
        long createdSeed = System.currentTimeMillis() / 1000L;
        repository.touchMissionReroll(type, createdSeed);
        return createdSeed;
    }

    private List<String> buildSharedSlotKeys(Map<String, List<MissionTemplate>> byGroup,
                                             MissionType type,
                                             String periodKey,
                                             int slot,
                                             String templateId,
                                             String currentGroup) {
        List<String> result = new ArrayList<String>();
        for (String group : byGroup.keySet()) {
            if (group.equalsIgnoreCase(currentGroup)) {
                continue;
            }
            result.add(missionKey(type, periodKey, group, slot, templateId));
        }
        return result;
    }

    private String missionKey(MissionType type, String periodKey, String serverGroup, int slot, String templateId) {
        return type.name().toLowerCase(Locale.ROOT) + ":" + periodKey + ":" + serverGroup + ":" + slot + ":" + templateId;
    }

    private Map<Integer, MissionTemplate> resolveForcedDailySlots(Map<String, MissionTemplate> templates) {
        Map<Integer, MissionTemplate> result = new LinkedHashMap<Integer, MissionTemplate>();
        ConfigurationSection section = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "missions/daily.yml")).getConfigurationSection("forced-slots");
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            MissionTemplate template = templates.get(section.getString(key, ""));
            if (template == null) {
                continue;
            }
            result.put(Integer.valueOf(Integer.parseInt(key)), template);
        }
        return result;
    }

    private List<MissionTemplate> removeForcedTemplates(List<MissionTemplate> source, Map<Integer, MissionTemplate> forcedSlots) {
        if (forcedSlots.isEmpty()) {
            return new ArrayList<MissionTemplate>(source);
        }
        List<MissionTemplate> result = new ArrayList<MissionTemplate>();
        for (MissionTemplate template : source) {
            boolean forced = false;
            for (MissionTemplate forcedTemplate : forcedSlots.values()) {
                if (forcedTemplate.getId().equalsIgnoreCase(template.getId())) {
                    forced = true;
                    break;
                }
            }
            if (!forced) {
                result.add(template);
            }
        }
        return result;
    }

    private int currentSeasonWeek() {
        SeasonDefinition season = seasonService.getActiveSeason();
        LocalDate start = season.getStartsAt().toLocalDate();
        LocalDate now = LocalDate.now(zoneId);
        if (now.isBefore(start)) {
            return 1;
        }
        long days = Duration.between(start.atStartOfDay(), now.atStartOfDay()).toDays();
        return (int) (days / 7L) + 1;
    }

    private List<ActiveMission> sortMissions(List<ActiveMission> missions) {
        Collections.sort(missions, new Comparator<ActiveMission>() {
            @Override
            public int compare(ActiveMission left, ActiveMission right) {
                int week = Integer.compare(left.getWeekNumber(), right.getWeekNumber());
                if (week != 0) {
                    return week;
                }
                int group = left.getTemplate().getServerGroup().compareToIgnoreCase(right.getTemplate().getServerGroup());
                if (group != 0) {
                    return group;
                }
                return Integer.compare(left.getSlot(), right.getSlot());
            }
        });
        return missions;
    }

    private Map<String, MissionTemplate> loadTemplates(File file) {
        return loadTemplates(YamlConfiguration.loadConfiguration(file));
    }

    private Map<String, MissionTemplate> loadTemplates(YamlConfiguration configuration) {
        Map<String, MissionTemplate> result = new LinkedHashMap<String, MissionTemplate>();
        ConfigurationSection templates = configuration.getConfigurationSection("templates");
        if (templates == null) {
            return result;
        }
        for (String key : templates.getKeys(false)) {
            ConfigurationSection section = templates.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            MissionTemplate template = new MissionTemplate(
                    key,
                    MissionTriggerType.valueOf(section.getString("trigger", "CUSTOM").toUpperCase(Locale.ROOT)),
                    section.getString("server-group", "default"),
                    normalizeTarget(section.getString("target", "")),
                    section.getString("display-name", key),
                    section.getStringList("description"),
                    section.getInt("required", 1),
                    section.getInt("xp", 0),
                    MissionConditions.fromSection(section.getConfigurationSection("conditions")),
                    section.isConfigurationSection("item")
                            ? MenuItemSpec.fromSection(
                            section.getConfigurationSection("item"),
                            Material.PAPER,
                            section.getString("display-name", key),
                            section.getStringList("description")
                    )
                            : null
            );
            result.put(key, template);
        }
        return result;
    }

    public String resolveConditionValue(Player player, String value) {
        if (value == null) {
            return "";
        }
        String resolved = value;
        if (player != null) {
            resolved = resolved
                    .replace("%player_world%", player.getWorld().getName())
                    .replace("%player_x%", Double.toString(player.getLocation().getX()))
                    .replace("%player_y%", Double.toString(player.getLocation().getY()))
                    .replace("%player_z%", Double.toString(player.getLocation().getZ()))
                    .replace("%player_server_id%", settings.getServerIdentity().getServerId())
                    .replace("%player_server_group%", settings.getServerIdentity().getServerGroup());
        }
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                resolved = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, resolved);
            } catch (Throwable ignored) {
            }
        }
        return resolved;
    }

    private boolean matchesTarget(MissionTemplate template, String targetKey) {
        if (template.getTarget() == null || template.getTarget().isEmpty()) {
            return true;
        }
        if (targetKey == null || targetKey.isEmpty()) {
            return false;
        }
        return template.getTarget().equalsIgnoreCase(normalizeTarget(targetKey));
    }

    private String normalizeTarget(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String formatTarget(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private void sendMissionProgressMessage(Player player, ActiveMission mission, int previousProgress, int progress) {
        if (!plugin.getConfig().getBoolean("messages.mission-progress.enabled", true)) {
            return;
        }
        int required = Math.max(1, mission.getTemplate().getRequired());
        int previousPercent = Math.min(100, (int) Math.floor((previousProgress * 100.0D) / required));
        int percent = Math.min(100, (int) Math.floor((progress * 100.0D) / required));
        int notifiedPercent = highestCrossedThreshold(previousPercent, percent);
        if (notifiedPercent < 0) {
            return;
        }
        int remaining = Math.max(0, required - progress);
        String message = plugin.getConfig().getString(
                "messages.mission-progress.format",
                "{prefix}&7Progress for &e{mission}&7: &f{progress}&7/&f{required} &8(&e{percent}%&8)"
        );
        player.sendMessage(plugin.getTextUtils().color(
                message
                        .replace("{prefix}", plugin.getConfig().getString("messages.prefix", ""))
                        .replace("{mission}", mission.getTemplate().getDisplayName())
                        .replace("{progress}", Integer.toString(progress))
                        .replace("{required}", Integer.toString(required))
                        .replace("{percent}", Integer.toString(percent))
                        .replace("{notify_percent}", Integer.toString(notifiedPercent))
                        .replace("{remaining}", Integer.toString(remaining))
                        .replace("{xp}", Integer.toString(mission.getTemplate().getXp()))
        ));
    }

    private int highestCrossedThreshold(int previousPercent, int currentPercent) {
        List<Integer> thresholds = plugin.getConfig().getIntegerList("messages.mission-progress.notify-percentages");
        int matched = -1;
        for (Integer threshold : thresholds) {
            if (threshold == null) {
                continue;
            }
            int value = threshold.intValue();
            if (value <= previousPercent) {
                continue;
            }
            if (value > currentPercent) {
                continue;
            }
            if (value > matched) {
                matched = value;
            }
        }
        return matched;
    }
}
