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
import java.util.List;
import java.util.Locale;

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
        PlayerBattlePassProgress progress = playerProgressService.getProgress(player);
        SeasonDefinition season = seasonService.getActiveSeason();

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
        inventory.setItem(config.getInt("items.daily-slot", 10), createButton(config.getConfigurationSection("icons.daily"), player, "&eDaily missions", MenuView.DAILY, Material.CLOCK));
        inventory.setItem(config.getInt("items.weekly-slot", 12), createButton(config.getConfigurationSection("icons.weekly"), player, "&6Weekly missions", MenuView.WEEKLY, Material.COMPASS));
        inventory.setItem(config.getInt("items.global-slot", 14), createButton(config.getConfigurationSection("icons.global"), player, "&bGlobal missions", MenuView.GLOBAL, Material.BOOK));
        inventory.setItem(config.getInt("items.rewards-slot", 16), createButton(config.getConfigurationSection("icons.rewards"), player, "&aLevel rewards", MenuView.REWARDS, Material.CHEST));
        player.openInventory(inventory);
    }

    public void handleViewOpen(Player player, MenuView view) {
        handleViewOpen(player, view, 0);
    }

    public void handleViewOpen(Player player, MenuView view, int page) {
        Inventory inventory = Bukkit.createInventory(null, 27, placeholders.resolve(player, "&8" + titleFor(view)));
        try {
            if (view == MenuView.DAILY || view == MenuView.WEEKLY || view == MenuView.GLOBAL) {
                fillMissionView(player, inventory, missionTypeFor(view), page);
            } else if (view == MenuView.REWARDS) {
                fillRewardView(player, inventory, page);
            }
        } catch (SQLException exception) {
            inventory.setItem(13, createItem(Material.BARRIER,
                    placeholders.resolve(player, "&cUnable to load data"),
                    "menu:error",
                    null,
                    placeholders.resolve(player, "&7" + exception.getMessage())));
        }
        inventory.setItem(22, createItem(Material.ARROW, placeholders.resolve(player, "&aBack"), "menu:back", null, placeholders.resolve(player, "&7Return to the main battle pass menu")));
        player.openInventory(inventory);
    }

    public boolean isManagedInventory(String title) {
        if (title == null) {
            return false;
        }
        String stripped = org.bukkit.ChatColor.stripColor(title);
        return stripped != null && (stripped.contains("Battle Pass")
                || stripped.equalsIgnoreCase("daily missions")
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

    private ItemStack createButton(ConfigurationSection section, Player player, String name, MenuView view, Material fallbackMaterial) {
        return createConfiguredItem(
                player,
                MenuItemSpec.fromSection(section, fallbackMaterial, name, defaultList("&7Open " + titleFor(view))),
                "menu:view:" + view.name()
        );
    }

    private ItemStack createItem(Material material, String name, String customKey, Integer customModelData, String... loreLines) {
        List<String> lore = new ArrayList<String>();
        if (loreLines != null) {
            for (String line : loreLines) {
                lore.add(line);
            }
        }
        ItemStack item = itemUtils.createItem(material.name(), 1, customModelData, name, Collections.<String>emptyList(), lore, Collections.<String>emptyList());
        itemUtils.setCustomItemKey(item, customKey, name);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillMissionView(Player player, Inventory inventory, MissionType type, int page) throws SQLException {
        List<MissionView> missions = missionService.getMissionViews(player, type);
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
            inventory.setItem(slots[index], createItem(mission.isCompleted() ? Material.EMERALD_BLOCK : Material.PAPER,
                    placeholders.resolve(player, mission.getActiveMission().getTemplate().getDisplayName()),
                    "menu:mission:" + type.name() + ":" + (startIndex + index),
                    null,
                    resolveLore(player, lore)));
        }
        if (missions.isEmpty()) {
            inventory.setItem(13, createItem(Material.BARRIER,
                    placeholders.resolve(player, "&cNo active missions"),
                    "menu:mission:empty",
                    null,
                    placeholders.resolve(player, "&7Nothing is active for this page right now.")));
            return;
        }

        int maxPage = Math.max(0, (int) Math.ceil(missions.size() / 7.0D) - 1);
        if (page > 0) {
            inventory.setItem(9, createNavigationItem(player, Material.ARROW, "&ePrevious page",
                    Collections.singletonList("&7Open the previous mission page"),
                    "MISSION_NAV:" + type.name() + ":" + (page - 1)));
        }
        if (page < maxPage) {
            inventory.setItem(17, createNavigationItem(player, Material.ARROW, "&eNext page",
                    Collections.singletonList("&7Open the next mission page"),
                    "MISSION_NAV:" + type.name() + ":" + (page + 1)));
        }
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
                    configMaterial("rewards.menu.previous-page-material", Material.ARROW),
                    plugin.getConfig().getString("rewards.menu.previous-page-name", "&ePrevious page"),
                    plugin.getConfig().getStringList("rewards.menu.previous-page-lore"),
                    "NAV:" + (page - 1)
            ));
        }
        if (page < maxPage) {
            inventory.setItem(17, createNavigationItem(
                    player,
                    configMaterial("rewards.menu.next-page-material", Material.ARROW),
                    plugin.getConfig().getString("rewards.menu.next-page-name", "&eNext page"),
                    plugin.getConfig().getStringList("rewards.menu.next-page-lore"),
                    "NAV:" + (page + 1)
            ));
        }
    }

    private ItemStack createRewardItem(Player player, RewardView reward) {
        RewardTrack track = reward.getDefinition().getTrack();
        Material material;
        if (!reward.isReachedLevel()) {
            material = Material.GRAY_STAINED_GLASS_PANE;
        } else if (track == RewardTrack.PREMIUM && !reward.isPremiumAvailable()) {
            material = configMaterial("rewards.premium-locked-material", Material.BARRIER);
        } else if (reward.isClaimable()) {
            material = track == RewardTrack.PREMIUM ? Material.EMERALD : Material.CHEST;
        } else {
            material = Material.REDSTONE_BLOCK;
        }

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
        return createItem(material,
                placeholders.resolve(player, "&6Level " + reward.getDefinition().getLevel() + " &8- &f" + track.name().toLowerCase(Locale.ROOT)),
                "menu:claim:" + reward.getDefinition().getLevel() + ":" + track.name(),
                rewardItemCustomModelData(reward),
                resolveLore(player, lore));
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

    private ItemStack createNavigationItem(Player player, Material material, String name, List<String> lore, String action) {
        List<String> lines = new ArrayList<String>();
        lines.addAll(lore == null ? Collections.<String>emptyList() : lore);
        return createConfiguredItem(
                player,
                new MenuItemSpec(material, name, navigationCustomModelData(action), lines),
                "menu:" + action.toLowerCase(Locale.ROOT)
        );
    }

    private ItemStack createConfiguredItem(Player player, MenuItemSpec spec, String customKey) {
        return createItem(
                spec.getMaterial(),
                placeholders.resolve(player, spec.getName()),
                customKey,
                spec.getCustomModelData(),
                resolveLore(player, spec.getLore())
        );
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

    private Material configMaterial(String path, Material fallback) {
        Material material = Material.matchMaterial(plugin.getConfig().getString(path, fallback.name()));
        return material == null ? fallback : material;
    }

    private Integer configInt(String path) {
        if (!plugin.getConfig().contains(path)) {
            return null;
        }
        return Integer.valueOf(plugin.getConfig().getInt(path));
    }

    private List<String> defaultList(String... lines) {
        List<String> result = new ArrayList<String>();
        if (lines != null) {
            Collections.addAll(result, lines);
        }
        return result;
    }

    private Integer navigationCustomModelData(String action) {
        if (action.startsWith("NAV:")) {
            return action.equals("NAV:0") ? configInt("rewards.menu.previous-page-custom-model-data") : configInt("rewards.menu.next-page-custom-model-data");
        }
        if (action.startsWith("MISSION_NAV:")) {
            if (action.endsWith(":0")) {
                return configInt("missions.menu.previous-page-custom-model-data");
            }
            return configInt("missions.menu.next-page-custom-model-data");
        }
        return null;
    }

    private Integer rewardItemCustomModelData(RewardView reward) {
        if (!reward.isReachedLevel()) {
            return configInt("rewards.menu.locked-custom-model-data");
        }
        if (reward.getDefinition().getTrack() == RewardTrack.PREMIUM && !reward.isPremiumAvailable()) {
            return configInt("rewards.premium-locked-custom-model-data");
        }
        if (reward.isClaimable()) {
            return reward.getDefinition().getTrack() == RewardTrack.PREMIUM
                    ? configInt("rewards.menu.premium-claimable-custom-model-data")
                    : configInt("rewards.menu.free-claimable-custom-model-data");
        }
        return reward.getDefinition().getTrack() == RewardTrack.PREMIUM
                ? configInt("rewards.menu.premium-claimed-custom-model-data")
                : configInt("rewards.menu.free-claimed-custom-model-data");
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
}
