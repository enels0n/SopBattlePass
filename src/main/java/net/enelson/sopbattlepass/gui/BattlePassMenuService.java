package net.enelson.sopbattlepass.gui;

import net.enelson.sopbattlepass.SopBattlePass;
import net.enelson.sopbattlepass.config.PluginSettings;
import net.enelson.sopbattlepass.menu.MenuView;
import net.enelson.sopbattlepass.mission.MissionService;
import net.enelson.sopbattlepass.mission.MissionType;
import net.enelson.sopbattlepass.mission.MissionView;
import net.enelson.sopbattlepass.placeholder.BattlePassPlaceholders;
import net.enelson.sopbattlepass.player.PlayerBattlePassProgress;
import net.enelson.sopbattlepass.player.PlayerProgressService;
import net.enelson.sopbattlepass.reward.RewardClaimResult;
import net.enelson.sopbattlepass.reward.RewardService;
import net.enelson.sopbattlepass.reward.RewardTrack;
import net.enelson.sopbattlepass.reward.RewardView;
import net.enelson.sopbattlepass.season.SeasonDefinition;
import net.enelson.sopbattlepass.season.SeasonService;
import net.enelson.sopli.lib.SopLib;
import net.enelson.sopli.lib.item.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BattlePassMenuService {

    private static final String MENU_KEY = "SopBattlePass:";

    private final SopBattlePass plugin;
    private final BattlePassPlaceholders placeholders;
    private final PlayerProgressService playerProgressService;
    private final SeasonService seasonService;
    private final PluginSettings settings;
    private final MissionService missionService;
    private final RewardService rewardService;
    private final ItemUtils itemUtils;

    public BattlePassMenuService(SopBattlePass plugin,
                                 BattlePassPlaceholders placeholders,
                                 PlayerProgressService playerProgressService,
                                 SeasonService seasonService,
                                 PluginSettings settings,
                                 MissionService missionService,
                                 RewardService rewardService) {
        this.plugin = plugin;
        this.placeholders = placeholders;
        this.playerProgressService = playerProgressService;
        this.seasonService = seasonService;
        this.settings = settings;
        this.missionService = missionService;
        this.rewardService = rewardService;
        this.itemUtils = SopLib.getInstance().getItemUtils();
    }

    public void openMain(Player player) throws SQLException {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "menus/main.yml"));
        String title = placeholders.resolve(player, config.getString("title", "&8Battle Pass"));
        int size = normalizeSize(config.getInt("size", 27));
        Inventory inventory = Bukkit.createInventory(null, size, title);
        SeasonDefinition season = seasonService.getActiveSeason();
        if (!seasonService.hasSeasonStarted()) {
            openSeasonNotStarted(player, inventory, season);
            return;
        }

        PlayerBattlePassProgress progress = playerProgressService.getProgress(player);

        inventory.setItem(config.getInt("items.summary-slot", 13), createConfiguredItem(
                player,
                MenuItemSpec.fromSection(
                        config.getConfigurationSection("icons.summary"),
                        Material.NETHER_STAR,
                        "&6" + season.getName(),
                        defaultList(
                                "&7Level: &f" + progress.getLevel(),
                                "&7XP: &f" + progress.getXp(),
                                "&7Premium: &f" + (progress.isPremiumActive() ? "enabled" : "locked"),
                                "&7Server: &f" + settings.getServerIdentity().getServerId()
                        )
                ),
                "menu:summary"
        ));
        if (missionService.isCategoryEnabled(MissionType.DAILY)) {
            inventory.setItem(config.getInt("items.daily-slot", 10), createButton(config.getConfigurationSection("icons.daily"), player, "&eDaily missions", MenuView.DAILY, Material.CLOCK));
        }
        if (missionService.isCategoryEnabled(MissionType.WEEKLY)) {
            inventory.setItem(config.getInt("items.weekly-slot", 12), createButton(config.getConfigurationSection("icons.weekly"), player, "&6Weekly missions", MenuView.WEEKLY, Material.COMPASS));
        }
        if (missionService.isCategoryEnabled(MissionType.GLOBAL)) {
            inventory.setItem(config.getInt("items.global-slot", 14), createButton(config.getConfigurationSection("icons.global"), player, "&bGlobal missions", MenuView.GLOBAL, Material.BOOK));
        }
        inventory.setItem(config.getInt("items.rewards-slot", 16), createButton(config.getConfigurationSection("icons.rewards"), player, "&aLevel rewards", MenuView.REWARDS, Material.CHEST));
        player.openInventory(inventory);
    }

    public void handleViewOpen(Player player, MenuView view) {
        handleViewOpen(player, view, 0);
    }

    public void handleViewOpen(Player player, MenuView view, int page) {
        Inventory inventory = Bukkit.createInventory(null, 27, placeholders.resolve(player, "&8" + titleFor(view)));
        try {
            if (!seasonService.hasSeasonStarted()) {
                openSeasonNotStarted(player, inventory, seasonService.getActiveSeason());
                return;
            }
            if (view == MenuView.WEEKLY_WEEKS) {
                fillWeeklyWeekSelector(player, inventory, page);
            } else if (view == MenuView.DAILY || view == MenuView.WEEKLY || view == MenuView.GLOBAL) {
                MissionType missionType = missionTypeFor(view);
                if (!missionService.isCategoryEnabled(missionType)) {
                    inventory.setItem(13, createConfiguredSectionItem(player, "menu.shared.category-disabled", Material.BARRIER, "menu:mission:disabled", null));
                    inventory.setItem(22, createConfiguredSectionItem(player, "menu.shared.back-main", Material.ARROW, "menu:back", null));
                    player.openInventory(inventory);
                    return;
                }
                fillMissionView(player, inventory, missionType, page);
            } else if (view == MenuView.REWARDS) {
                fillRewardView(player, inventory, page);
            }
        } catch (SQLException exception) {
            Map<String, String> replacements = new LinkedHashMap<String, String>();
            replacements.put("error", exception.getMessage());
            inventory.setItem(13, createConfiguredSectionItem(player, "menu.shared.error", Material.BARRIER, "menu:error", replacements));
        }
        inventory.setItem(22, createConfiguredSectionItem(player, "menu.shared.back-main", Material.ARROW, "menu:back", null));
        player.openInventory(inventory);
    }

    private void openSeasonNotStarted(Player player, Inventory inventory, SeasonDefinition season) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("season.menu.not-started");
        MenuItemSpec baseSpec = MenuItemSpec.fromSection(
                section,
                Material.CLOCK,
                "",
                Collections.<String>emptyList()
        );
        String seasonStart = season.getStartsAt().toString().replace('T', ' ');
        List<String> lore = new ArrayList<String>();
        for (String line : baseSpec.getLore()) {
            lore.add(line
                    .replace("{season_name}", season.getName())
                    .replace("{season_start}", seasonStart));
        }
        MenuItemSpec spec = new MenuItemSpec(
                baseSpec.getMaterialSpec(),
                baseSpec.getFallbackMaterial(),
                baseSpec.getName()
                        .replace("{season_name}", season.getName())
                        .replace("{season_start}", seasonStart),
                baseSpec.getCustomModelData(),
                lore
        );
        inventory.setItem(13, createConfiguredItem(player, spec, "menu:season:not_started"));
        player.openInventory(inventory);
    }

    public boolean isManagedInventory(String title) {
        if (title == null) {
            return false;
        }
        String stripped = org.bukkit.ChatColor.stripColor(title);
        return stripped != null && (stripped.contains("Battle Pass")
                || stripped.toLowerCase(Locale.ROOT).startsWith("week ")
                || stripped.equalsIgnoreCase("daily missions")
                || stripped.equalsIgnoreCase("weekly weeks")
                || stripped.equalsIgnoreCase("weekly missions")
                || stripped.equalsIgnoreCase("global missions")
                || stripped.equalsIgnoreCase("level rewards"));
    }

    public MenuView resolveView(String title) {
        if (title == null) {
            return MenuView.MAIN;
        }
        String upper = title.toUpperCase();
        if (upper.contains("DAILY")) {
            return MenuView.DAILY;
        }
        if (upper.contains("WEEKLY WEEKS")) {
            return MenuView.WEEKLY_WEEKS;
        }
        if (upper.contains("WEEKLY")) {
            return MenuView.WEEKLY;
        }
        if (upper.contains("GLOBAL")) {
            return MenuView.GLOBAL;
        }
        if (upper.contains("REWARDS")) {
            return MenuView.REWARDS;
        }
        return MenuView.MAIN;
    }

    public void handleRewardClaim(Player player, int level, RewardTrack track) {
        try {
            RewardClaimResult result = rewardService.claim(player, level, track);
            String fallback = result.getMessage();
            String key = result.isSuccess() ? "messages.reward-claimed" : "messages.reward-claim-failed";
            String text = plugin.getConfig().getString(key, fallback)
                    .replace("{level}", Integer.toString(level))
                    .replace("{track}", track.name().toLowerCase(Locale.ROOT));
            player.sendMessage(placeholders.resolve(player, text));
            handleViewOpen(player, MenuView.REWARDS, pageForLevel(level));
        } catch (SQLException exception) {
            player.sendMessage(placeholders.resolve(player, "&cReward claim failed: " + exception.getMessage()));
        }
    }

    public void handleRewardPage(Player player, int page) {
        handleViewOpen(player, MenuView.REWARDS, Math.max(0, page));
    }

    public void handleMissionPage(Player player, MenuView view, int page) {
        handleViewOpen(player, view, Math.max(0, page));
    }

    public void handleWeeklyWeekSelectorPage(Player player, int page) {
        handleViewOpen(player, MenuView.WEEKLY_WEEKS, Math.max(0, page));
    }

    public void handleWeeklyWeekPage(Player player, int weekNumber, int page) {
        Inventory inventory = Bukkit.createInventory(null, 27, placeholders.resolve(player, "&8Week " + weekNumber));
        try {
            if (!seasonService.hasSeasonStarted()) {
                openSeasonNotStarted(player, inventory, seasonService.getActiveSeason());
                return;
            }
            if (!missionService.isCategoryEnabled(MissionType.WEEKLY)) {
                inventory.setItem(13, createConfiguredSectionItem(player, "menu.shared.category-disabled", Material.BARRIER, "menu:mission:disabled", null));
            } else {
                fillWeeklyMissionView(player, inventory, weekNumber, Math.max(0, page));
            }
        } catch (SQLException exception) {
            Map<String, String> replacements = new LinkedHashMap<String, String>();
            replacements.put("error", exception.getMessage());
            inventory.setItem(13, createConfiguredSectionItem(player, "menu.shared.error", Material.BARRIER, "menu:error", replacements));
        }
        inventory.setItem(22, createConfiguredSectionItem(player, "missions.menu.back-to-weeks", Material.ARROW, "menu:weekly_selector", null));
        player.openInventory(inventory);
    }

    private ItemStack createButton(ConfigurationSection section, Player player, String name, MenuView view, Material fallbackMaterial) {
        return createConfiguredItem(
                player,
                MenuItemSpec.fromSection(section, fallbackMaterial, name, defaultList("&7Open " + titleFor(view))),
                "menu:view:" + view.name()
        );
    }

    private ItemStack createItem(Material material, String name, String customKey, Integer customModelData, String... loreLines) {
        return createItem(material.name(), material, name, customKey, customModelData, loreLines);
    }

    private ItemStack createItem(String materialSpec, Material fallbackMaterial, String name, String customKey, Integer customModelData, String... loreLines) {
        List<String> lore = new ArrayList<String>();
        if (loreLines != null) {
            for (String line : loreLines) {
                lore.add(line);
            }
        }
        ItemStack item = createBaseItem(materialSpec, fallbackMaterial, customModelData);
        itemUtils.setCustomItemKey(item, customKey, name);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getTextUtils().color(name));
            meta.setLore(plugin.getTextUtils().color(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillMissionView(Player player, Inventory inventory, MissionType type, int page) throws SQLException {
        List<MissionView> missions = missionService.getMissionViews(player, type);
        fillMissionView(player, inventory, type, missions, page, false, 0);
    }

    private void fillWeeklyMissionView(Player player, Inventory inventory, int weekNumber, int page) throws SQLException {
        List<MissionView> filtered = new ArrayList<MissionView>();
        for (MissionView mission : missionService.getMissionViews(player, MissionType.WEEKLY)) {
            if (mission.getActiveMission().getWeekNumber() == weekNumber) {
                filtered.add(mission);
            }
        }
        fillMissionView(player, inventory, MissionType.WEEKLY, filtered, page, true, weekNumber);
    }

    private void fillMissionView(Player player,
                                 Inventory inventory,
                                 MissionType type,
                                 List<MissionView> missions,
                                 int page,
                                 boolean weeklyScoped,
                                 int weekNumber) {
        int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16};
        int startIndex = page * slots.length;
        for (int index = 0; index < slots.length && (startIndex + index) < missions.size(); index++) {
            MissionView mission = missions.get(startIndex + index);
            List<String> lore = new ArrayList<String>(mission.getResolvedDescription());
            lore.add(placeholders.resolve(player, "&7Progress: &f" + mission.getProgress() + "/" + mission.getActiveMission().getTemplate().getRequired()));
            lore.add(placeholders.resolve(player, "&7XP: &f" + mission.getActiveMission().getTemplate().getXp()));
            if (mission.getActiveMission().getWeekNumber() > 0) {
                lore.add(placeholders.resolve(player, "&7Week: &f" + mission.getActiveMission().getWeekNumber()));
            }
            lore.add(placeholders.resolve(player, mission.isCompleted() ? "&aCompleted" : "&eIn progress"));
            inventory.setItem(slots[index], createMissionItem(player, mission, type, startIndex + index, lore));
        }
        if (missions.isEmpty()) {
            inventory.setItem(13, createConfiguredSectionItem(player, "missions.menu.empty", Material.BARRIER, "menu:mission:empty", null));
            return;
        }

        int maxPage = Math.max(0, (int) Math.ceil(missions.size() / 7.0D) - 1);
        if (page > 0) {
            inventory.setItem(9, createNavigationItem(
                    player,
                    plugin.getConfig().getConfigurationSection("missions.menu.previous-page"),
                    Material.ARROW,
                    "&ePrevious page",
                    Collections.singletonList("&7Open the previous mission page"),
                    weeklyScoped
                            ? "WEEKLY_PAGE:" + weekNumber + ":" + (page - 1)
                            : "MISSION_NAV:" + type.name() + ":" + (page - 1)
            ));
        }
        if (page < maxPage) {
            inventory.setItem(17, createNavigationItem(
                    player,
                    plugin.getConfig().getConfigurationSection("missions.menu.next-page"),
                    Material.ARROW,
                    "&eNext page",
                    Collections.singletonList("&7Open the next mission page"),
                    weeklyScoped
                            ? "WEEKLY_PAGE:" + weekNumber + ":" + (page + 1)
                            : "MISSION_NAV:" + type.name() + ":" + (page + 1)
            ));
        }
    }

    private void fillWeeklyWeekSelector(Player player, Inventory inventory, int page) throws SQLException {
        List<Integer> weeks = collectWeeklyWeeks(player);
        int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16};
        int startIndex = page * slots.length;
        for (int index = 0; index < slots.length && (startIndex + index) < weeks.size(); index++) {
            int weekNumber = weeks.get(startIndex + index);
            inventory.setItem(slots[index], createWeeklyWeekItem(player, weekNumber));
        }
        if (weeks.isEmpty()) {
            inventory.setItem(13, createConfiguredSectionItem(player, "missions.menu.week-selector-empty", Material.BARRIER, "menu:weekly:empty", null));
            return;
        }

        int maxPage = Math.max(0, (int) Math.ceil(weeks.size() / 7.0D) - 1);
        if (page > 0) {
            inventory.setItem(9, createNavigationItem(
                    player,
                    plugin.getConfig().getConfigurationSection("missions.menu.previous-page"),
                    Material.ARROW,
                    "&ePrevious page",
                    Collections.singletonList("&7Open the previous week page"),
                    "WEEKLY_WEEKS_NAV:" + (page - 1)
            ));
        }
        if (page < maxPage) {
            inventory.setItem(17, createNavigationItem(
                    player,
                    plugin.getConfig().getConfigurationSection("missions.menu.next-page"),
                    Material.ARROW,
                    "&eNext page",
                    Collections.singletonList("&7Open the next week page"),
                    "WEEKLY_WEEKS_NAV:" + (page + 1)
            ));
        }
    }

    private ItemStack createWeeklyWeekItem(Player player, int weekNumber) {
        MenuItemSpec baseSpec = MenuItemSpec.fromSection(
                plugin.getConfig().getConfigurationSection("missions.menu.week-selector"),
                Material.CLOCK,
                "&6Week {week}",
                defaultList("&7Open missions for week &f{week}")
        );
        List<String> lore = new ArrayList<String>();
        for (String line : baseSpec.getLore()) {
            lore.add(line.replace("{week}", Integer.toString(weekNumber)));
        }
        MenuItemSpec resolvedSpec = new MenuItemSpec(
                baseSpec.getMaterialSpec(),
                baseSpec.getFallbackMaterial(),
                baseSpec.getName().replace("{week}", Integer.toString(weekNumber)),
                baseSpec.getCustomModelData(),
                lore
        );
        return createConfiguredItem(player, resolvedSpec, "menu:weekly_week:" + weekNumber);
    }

    private void fillRewardView(Player player, Inventory inventory, int page) throws SQLException {
        List<RewardView> rewardViews = rewardService.getRewardViews(player);
        int pageBaseLevel = page * 9 + 1;
        for (int offset = 0; offset < 9; offset++) {
            int level = pageBaseLevel + offset;
            RewardView freeReward = findRewardView(rewardViews, level, RewardTrack.FREE);
            RewardView premiumReward = findRewardView(rewardViews, level, RewardTrack.PREMIUM);

            inventory.setItem(offset, freeReward == null
                    ? createConfiguredEmptyItem(player)
                    : createRewardItem(player, freeReward));
            inventory.setItem(18 + offset, premiumReward == null
                    ? createConfiguredEmptyItem(player)
                    : createRewardItem(player, premiumReward));
        }

        int maxLevel = maxRewardLevel(rewardViews);
        int maxPage = Math.max(0, (int) Math.ceil(maxLevel / 9.0D) - 1);
        if (page > 0) {
            inventory.setItem(9, createNavigationItem(
                    player,
                    plugin.getConfig().getConfigurationSection("rewards.menu.previous-page"),
                    Material.ARROW,
                    "&ePrevious page",
                    plugin.getConfig().getStringList("rewards.menu.previous-page.lore"),
                    "NAV:" + (page - 1)
            ));
        }
        if (page < maxPage) {
            inventory.setItem(17, createNavigationItem(
                    player,
                    plugin.getConfig().getConfigurationSection("rewards.menu.next-page"),
                    Material.ARROW,
                    "&eNext page",
                    plugin.getConfig().getStringList("rewards.menu.next-page.lore"),
                    "NAV:" + (page + 1)
            ));
        }
    }

    private ItemStack createRewardItem(Player player, RewardView reward) {
        RewardTrack track = reward.getDefinition().getTrack();
        MenuItemSpec stateSpec = rewardItemSpec(reward);
        List<String> lore = new ArrayList<String>();
        lore.add("&8" + MENU_KEY + "CLAIM:" + reward.getDefinition().getLevel() + ":" + track.name());
        lore.add("&7Track: &f" + track.name().toLowerCase(Locale.ROOT));
        lore.add("&7Required level: &f" + reward.getDefinition().getLevel());
        lore.add("&7Used claims: &f" + reward.getUsedClaims() + "/" + formatClaimLimit(reward.getDefinition().getClaimLimit()));
        lore.add("&7Claimed on: &f" + (reward.getClaimedServerIds().isEmpty() ? "-" : joinClaims(reward.getClaimedServerIds())));
        lore.add(reward.isReachedLevel() ? "&aLevel reached" : "&cLevel locked");
        if (track == RewardTrack.PREMIUM) {
            lore.add(reward.isPremiumAvailable() ? "&aPremium active" : "&cPremium locked");
        }
        lore.add(reward.isClaimable() ? "&eClick to claim on this server" : "&7Cannot claim on this server");
        List<String> finalLore = mergeConfiguredLore(stateSpec.getLore(), lore);
        return createItem(stateSpec.getMaterialSpec(),
                stateSpec.getFallbackMaterial(),
                placeholders.resolve(player, stateSpec.getName()
                        .replace("{level}", Integer.toString(reward.getDefinition().getLevel()))
                        .replace("{track}", track.name().toLowerCase(Locale.ROOT))),
                "menu:claim:" + reward.getDefinition().getLevel() + ":" + track.name(),
                stateSpec.getCustomModelData(),
                resolveLore(player, finalLore));
    }

    private ItemStack createConfiguredEmptyItem(Player player) {
        return createConfiguredItem(
                player,
                MenuItemSpec.fromSection(
                        plugin.getConfig().getConfigurationSection("rewards.menu.empty-slot"),
                        Material.BLACK_STAINED_GLASS_PANE,
                        " ",
                        Collections.<String>emptyList()
                ),
                "menu:empty"
        );
    }

    private ItemStack createNavigationItem(Player player, ConfigurationSection section, Material fallbackMaterial, String fallbackName, List<String> fallbackLore, String action) {
        return createConfiguredItem(
                player,
                MenuItemSpec.fromSection(section, fallbackMaterial, fallbackName, fallbackLore),
                "menu:" + action.toLowerCase(Locale.ROOT)
        );
    }

    private ItemStack createMissionItem(Player player, MissionView mission, MissionType type, int missionIndex, List<String> lore) {
        Material fallbackMaterial = mission.isCompleted() ? Material.EMERALD_BLOCK : Material.PAPER;
        MenuItemSpec spec = MenuItemSpec.fromSection(
                null,
                fallbackMaterial,
                mission.getActiveMission().getTemplate().getDisplayName(),
                lore
        );
        MenuItemSpec configured = mission.getActiveMission().getTemplate().getItemSpec();
        if (configured != null) {
            spec = new MenuItemSpec(
                    configured.getMaterialSpec(),
                    fallbackMaterial,
                    configured.getName(),
                    configured.getCustomModelData(),
                    mergeMissionLore(configured.getLore(), lore)
            );
        }
        return createConfiguredItem(player, spec, "menu:mission:" + type.name() + ":" + missionIndex);
    }

    private ItemStack createConfiguredItem(Player player, MenuItemSpec spec, String customKey) {
        return createItem(
                spec.getMaterialSpec(),
                spec.getFallbackMaterial(),
                placeholders.resolve(player, spec.getName()),
                customKey,
                spec.getCustomModelData(),
                resolveLore(player, spec.getLore())
        );
    }

    private ItemStack createConfiguredSectionItem(Player player,
                                                  String path,
                                                  Material fallbackMaterial,
                                                  String customKey,
                                                  Map<String, String> replacements) {
        MenuItemSpec spec = MenuItemSpec.fromSection(
                plugin.getConfig().getConfigurationSection(path),
                fallbackMaterial,
                "",
                Collections.<String>emptyList()
        );
        return createConfiguredItem(player, applyReplacements(spec, replacements), customKey);
    }

    private ItemStack createBaseItem(String materialSpec, Material fallbackMaterial, Integer customModelData) {
        ItemStack item;
        if (materialSpec != null && materialSpec.regionMatches(true, 0, "HEAD:", 0, 5)) {
            String texture = materialSpec.substring(5);
            item = itemUtils.getHeadTexture(texture, null);
            if (item == null) {
                item = new ItemStack(Material.PLAYER_HEAD);
            }
        } else {
            Material material = Material.matchMaterial(materialSpec == null ? fallbackMaterial.name() : materialSpec);
            item = new ItemStack(material == null ? fallbackMaterial : material);
        }
        if (customModelData != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private RewardView findRewardView(List<RewardView> rewardViews, int level, RewardTrack track) {
        for (RewardView rewardView : rewardViews) {
            if (rewardView.getDefinition().getLevel() == level && rewardView.getDefinition().getTrack() == track) {
                return rewardView;
            }
        }
        return null;
    }

    private int maxRewardLevel(List<RewardView> rewardViews) {
        int max = 0;
        for (RewardView rewardView : rewardViews) {
            if (rewardView.getDefinition().getLevel() > max) {
                max = rewardView.getDefinition().getLevel();
            }
        }
        return max;
    }

    private String[] resolveLore(Player player, List<String> lore) {
        String[] result = new String[lore.size()];
        for (int index = 0; index < lore.size(); index++) {
            result[index] = placeholders.resolve(player, lore.get(index));
        }
        return result;
    }

    private String formatClaimLimit(int limit) {
        return limit <= 0 ? "unlimited" : Integer.toString(limit);
    }

    private String joinClaims(List<String> claimedServerIds) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < claimedServerIds.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(claimedServerIds.get(index));
        }
        return builder.toString();
    }

    private List<String> defaultList(String... lines) {
        List<String> result = new ArrayList<String>();
        if (lines != null) {
            Collections.addAll(result, lines);
        }
        return result;
    }

    private MenuItemSpec applyReplacements(MenuItemSpec spec, Map<String, String> replacements) {
        if (replacements == null || replacements.isEmpty()) {
            return spec;
        }
        List<String> lore = new ArrayList<String>();
        for (String line : spec.getLore()) {
            lore.add(replaceTokens(line, replacements));
        }
        return new MenuItemSpec(
                spec.getMaterialSpec(),
                spec.getFallbackMaterial(),
                replaceTokens(spec.getName(), replacements),
                spec.getCustomModelData(),
                lore
        );
    }

    private String replaceTokens(String input, Map<String, String> replacements) {
        if (input == null || replacements == null || replacements.isEmpty()) {
            return input;
        }
        String result = input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private MenuItemSpec rewardItemSpec(RewardView reward) {
        if (!reward.isReachedLevel()) {
            return MenuItemSpec.fromSection(
                    plugin.getConfig().getConfigurationSection("rewards.menu.states.locked"),
                    Material.GRAY_STAINED_GLASS_PANE,
                    "&7Level {level} - {track}",
                    defaultList("&7Reach this level to unlock this reward.")
            );
        }
        if (reward.getDefinition().getTrack() == RewardTrack.PREMIUM && !reward.isPremiumAvailable()) {
            return MenuItemSpec.fromSection(
                    plugin.getConfig().getConfigurationSection("rewards.premium-locked"),
                    Material.BARRIER,
                    "&cLevel {level} - premium",
                    defaultList("&7Reach this level and unlock premium", "&7to claim the reward later.")
            );
        }
        if (reward.isClaimable()) {
            return MenuItemSpec.fromSection(
                    plugin.getConfig().getConfigurationSection(reward.getDefinition().getTrack() == RewardTrack.PREMIUM
                            ? "rewards.menu.states.premium-claimable"
                            : "rewards.menu.states.free-claimable"),
                    reward.getDefinition().getTrack() == RewardTrack.PREMIUM ? Material.EMERALD : Material.CHEST,
                    "&aLevel {level} - {track}",
                    defaultList("&eClick to claim this reward on this server.")
            );
        }
        return MenuItemSpec.fromSection(
                plugin.getConfig().getConfigurationSection(reward.getDefinition().getTrack() == RewardTrack.PREMIUM
                        ? "rewards.menu.states.premium-claimed"
                        : "rewards.menu.states.free-claimed"),
                Material.REDSTONE_BLOCK,
                "&cLevel {level} - {track}",
                defaultList("&7This reward is not currently claimable on this server.")
        );
    }

    private List<String> mergeMissionLore(List<String> baseLore, List<String> runtimeLore) {
        return mergeConfiguredLore(baseLore, runtimeLore);
    }

    private List<String> mergeConfiguredLore(List<String> baseLore, List<String> runtimeLore) {
        if (baseLore == null || baseLore.isEmpty()) {
            return runtimeLore;
        }
        List<String> merged = new ArrayList<String>(baseLore);
        merged.add("");
        merged.addAll(runtimeLore);
        return merged;
    }

    private int pageForLevel(int level) {
        return Math.max(0, (level - 1) / 9);
    }

    private int normalizeSize(int size) {
        if (size < 9) {
            return 9;
        }
        int rows = size / 9;
        if (size % 9 != 0) {
            rows = rows + 1;
        }
        rows = Math.max(1, Math.min(6, rows));
        return rows * 9;
    }

    private String titleFor(MenuView view) {
        if (view == MenuView.DAILY) {
            return "daily missions";
        }
        if (view == MenuView.WEEKLY_WEEKS) {
            return "weekly weeks";
        }
        if (view == MenuView.WEEKLY) {
            return "weekly missions";
        }
        if (view == MenuView.GLOBAL) {
            return "global missions";
        }
        if (view == MenuView.REWARDS) {
            return "level rewards";
        }
        return "battle pass";
    }

    private MissionType missionTypeFor(MenuView view) {
        if (view == MenuView.DAILY) {
            return MissionType.DAILY;
        }
        if (view == MenuView.WEEKLY) {
            return MissionType.WEEKLY;
        }
        return MissionType.GLOBAL;
    }

    private List<Integer> collectWeeklyWeeks(Player player) throws SQLException {
        Set<Integer> weeks = new LinkedHashSet<Integer>();
        for (MissionView mission : missionService.getMissionViews(player, MissionType.WEEKLY)) {
            if (mission.getActiveMission().getWeekNumber() > 0) {
                weeks.add(Integer.valueOf(mission.getActiveMission().getWeekNumber()));
            }
        }
        return new ArrayList<Integer>(weeks);
    }
}
