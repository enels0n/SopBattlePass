package net.enelson.sopbattlepass.player;

import net.enelson.sopbattlepass.config.PluginSettings;
import net.enelson.sopbattlepass.season.SeasonDefinition;
import net.enelson.sopbattlepass.season.SeasonService;
import net.enelson.sopbattlepass.storage.BattlePassRepository;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public final class PlayerProgressService {

    private final BattlePassRepository repository;
    private final SeasonService seasonService;
    private final PluginSettings settings;

    public PlayerProgressService(BattlePassRepository repository, SeasonService seasonService, PluginSettings settings) {
        this.repository = repository;
        this.seasonService = seasonService;
        this.settings = settings;
    }

    public PlayerBattlePassProgress getProgress(OfflinePlayer player) throws SQLException {
        SeasonDefinition season = seasonService.getActiveSeason();
        PlayerBattlePassProgress stored = repository.findProgress(player.getUniqueId().toString(), season.getId());
        if (stored == null) {
            boolean premium = resolvePremium(player);
            PlayerBattlePassProgress created = new PlayerBattlePassProgress(player.getUniqueId().toString(), season.getId(), 0, 1, premium);
            repository.saveProgress(created);
            return created;
        }
        boolean premium = stored.isPremiumActive() || resolvePremium(player);
        if (premium != stored.isPremiumActive()) {
            PlayerBattlePassProgress updated = new PlayerBattlePassProgress(stored.getPlayerUuid(), stored.getSeasonId(), stored.getXp(), stored.getLevel(), premium);
            repository.saveProgress(updated);
            return updated;
        }
        return stored;
    }

    public PlayerBattlePassProgress addXp(OfflinePlayer player, int rawAmount) throws SQLException {
        PlayerBattlePassProgress current = getProgress(player);
        int amount = Math.max(0, rawAmount);
        int adjustedAmount = (int) Math.round(amount * settings.getGlobalXpMultiplier());
        int xp = current.getXp() + adjustedAmount;
        SeasonDefinition season = seasonService.getActiveSeason();
        int level = season.resolveLevelForXp(xp);
        PlayerBattlePassProgress updated = new PlayerBattlePassProgress(current.getPlayerUuid(), current.getSeasonId(), xp, level, current.isPremiumActive());
        repository.saveProgress(updated);
        return updated;
    }

    public PlayerBattlePassProgress setPremium(OfflinePlayer player, boolean premium) throws SQLException {
        PlayerBattlePassProgress current = getProgress(player);
        PlayerBattlePassProgress updated = new PlayerBattlePassProgress(current.getPlayerUuid(), current.getSeasonId(), current.getXp(), current.getLevel(), premium);
        repository.saveProgress(updated);
        return updated;
    }

    private boolean resolvePremium(OfflinePlayer player) {
        if (settings.isPremiumPermissionEnabled() && player instanceof Player && ((Player) player).hasPermission(settings.getPremiumPermissionNode())) {
            return true;
        }
        return settings.isPremiumSeasonFlagEnabled() && false;
    }
}
