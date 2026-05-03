package net.enelson.sopbattlepass;

import net.enelson.sopbattlepass.command.BattlePassCommand;
import net.enelson.sopbattlepass.config.PluginSettings;
import net.enelson.sopbattlepass.integration.AxEnvoyHook;
import net.enelson.sopbattlepass.integration.BreweryXHook;
import net.enelson.sopbattlepass.integration.CustomFishingHook;
import net.enelson.sopbattlepass.gui.BattlePassMenuListener;
import net.enelson.sopbattlepass.gui.BattlePassMenuService;
import net.enelson.sopbattlepass.integration.PinataPartyHook;
import net.enelson.sopbattlepass.mission.MissionListener;
import net.enelson.sopbattlepass.mission.MissionPlaytimeTask;
import net.enelson.sopbattlepass.mission.MissionService;
import net.enelson.sopbattlepass.placeholder.BattlePassPlaceholders;
import net.enelson.sopbattlepass.placeholder.SopBattlePassExpansion;
import net.enelson.sopbattlepass.player.PlayerProgressService;
import net.enelson.sopbattlepass.reward.RewardService;
import net.enelson.sopbattlepass.season.SeasonDefinition;
import net.enelson.sopbattlepass.season.SeasonService;
import net.enelson.sopbattlepass.storage.BattlePassRepository;
import net.enelson.sopbattlepass.storage.DatabaseFactory;
import net.enelson.sopbattlepass.storage.SqlBattlePassRepository;
import net.enelson.sopli.lib.database.SopDatabase;
import net.enelson.sopli.lib.text.TextUtils;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SopBattlePass extends JavaPlugin {

    private final TextUtils textUtils = new TextUtils();

    private PluginSettings settings;
    private SeasonService seasonService;
    private SopDatabase database;
    private BattlePassRepository repository;
    private PlayerProgressService playerProgressService;
    private MissionService missionService;
    private MissionPlaytimeTask missionPlaytimeTask;
    private RewardService rewardService;
    private BattlePassPlaceholders battlePassPlaceholders;
    private BattlePassMenuService menuService;
    private SopBattlePassExpansion expansion;
    private PinataPartyHook pinataPartyHook;
    private CustomFishingHook customFishingHook;
    private BreweryXHook breweryXHook;
    private AxEnvoyHook axEnvoyHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("missions/daily.yml", false);
        saveResource("missions/weekly.yml", false);
        saveResource("missions/global.yml", false);
        saveResource("seasons/season-1.yml", false);
        saveResource("menus/main.yml", false);

        try {
            loadPlugin();
        } catch (Exception exception) {
            getLogger().severe("Unable to enable SopBattlePass: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
        if (missionPlaytimeTask != null) {
            missionPlaytimeTask.stop();
            missionPlaytimeTask = null;
        }
        if (axEnvoyHook != null) {
            axEnvoyHook.unregister();
            axEnvoyHook = null;
        }
        if (breweryXHook != null) {
            breweryXHook.unregister();
            breweryXHook = null;
        }
        if (customFishingHook != null) {
            customFishingHook.unregister();
            customFishingHook = null;
        }
        if (pinataPartyHook != null) {
            pinataPartyHook.unregister();
            pinataPartyHook = null;
        }
        if (database != null) {
            database.close();
            database = null;
        }
    }

    public void reloadPlugin() throws Exception {
        reloadConfig();
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
        if (database != null) {
            database.close();
            database = null;
        }
        if (missionPlaytimeTask != null) {
            missionPlaytimeTask.stop();
            missionPlaytimeTask = null;
        }
        if (axEnvoyHook != null) {
            axEnvoyHook.unregister();
            axEnvoyHook = null;
        }
        if (breweryXHook != null) {
            breweryXHook.unregister();
            breweryXHook = null;
        }
        if (customFishingHook != null) {
            customFishingHook.unregister();
            customFishingHook = null;
        }
        if (pinataPartyHook != null) {
            pinataPartyHook.unregister();
            pinataPartyHook = null;
        }
        loadPlugin();
    }

    private void loadPlugin() throws Exception {
        this.settings = PluginSettings.load(this);
        this.seasonService = new SeasonService(this, settings);
        SeasonDefinition activeSeason = seasonService.getActiveSeason();
        this.database = new DatabaseFactory(this, settings).create();
        this.repository = new SqlBattlePassRepository(database, settings, activeSeason);
        repository.initialize();
        this.playerProgressService = new PlayerProgressService(repository, seasonService, settings);
        this.missionService = new MissionService(this, repository, seasonService, playerProgressService, settings);
        this.missionPlaytimeTask = new MissionPlaytimeTask(this, missionService);
        this.rewardService = new RewardService(this, repository, playerProgressService, settings);
        this.battlePassPlaceholders = new BattlePassPlaceholders(this, seasonService, playerProgressService, settings);
        this.menuService = new BattlePassMenuService(this, battlePassPlaceholders, playerProgressService, seasonService, settings, missionService, rewardService);

        BattlePassCommand commandExecutor = new BattlePassCommand(this, playerProgressService, menuService, battlePassPlaceholders, repository, missionService);
        PluginCommand command = getCommand("sopbattlepass");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }

        getServer().getPluginManager().registerEvents(new BattlePassMenuListener(menuService), this);
        getServer().getPluginManager().registerEvents(new MissionListener(missionService), this);
        this.missionPlaytimeTask.start();
        this.breweryXHook = new BreweryXHook(this, missionService);
        if (breweryXHook.register()) {
            getLogger().info("Hooked into BreweryX.");
        }
        this.axEnvoyHook = new AxEnvoyHook(this, missionService);
        if (axEnvoyHook.register()) {
            getLogger().info("Hooked into AxEnvoy.");
        }
        this.customFishingHook = new CustomFishingHook(this, missionService);
        if (customFishingHook.register()) {
            getLogger().info("Hooked into CustomFishing.");
        }
        this.pinataPartyHook = new PinataPartyHook(this, missionService);
        if (pinataPartyHook.register()) {
            getLogger().info("Hooked into PinataParty.");
        }
        registerPlaceholderExpansion();
    }

    private void registerPlaceholderExpansion() {
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }

        this.expansion = new SopBattlePassExpansion(this, battlePassPlaceholders);
        this.expansion.register();
    }

    public TextUtils getTextUtils() {
        return textUtils;
    }
}
