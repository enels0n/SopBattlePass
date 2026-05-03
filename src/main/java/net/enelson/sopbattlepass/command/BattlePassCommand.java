package net.enelson.sopbattlepass.command;

import net.enelson.sopbattlepass.SopBattlePass;
import net.enelson.sopbattlepass.gui.BattlePassMenuService;
import net.enelson.sopbattlepass.mission.MissionService;
import net.enelson.sopbattlepass.mission.MissionType;
import net.enelson.sopbattlepass.placeholder.BattlePassPlaceholders;
import net.enelson.sopbattlepass.player.PlayerProgressService;
import net.enelson.sopbattlepass.storage.BattlePassRepository;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BattlePassCommand implements CommandExecutor, TabCompleter {

    private final SopBattlePass plugin;
    private final PlayerProgressService playerProgressService;
    private final BattlePassMenuService menuService;
    private final BattlePassPlaceholders placeholders;
    private final BattlePassRepository repository;
    private final MissionService missionService;

    public BattlePassCommand(SopBattlePass plugin,
                             PlayerProgressService playerProgressService,
                             BattlePassMenuService menuService,
                             BattlePassPlaceholders placeholders,
                             BattlePassRepository repository,
                             MissionService missionService) {
        this.plugin = plugin;
        this.playerProgressService = playerProgressService;
        this.menuService = menuService;
        this.placeholders = placeholders;
        this.repository = repository;
        this.missionService = missionService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.player-only")));
                    return true;
                }
                menuService.openMain((Player) sender);
                sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.open-self")));
                return true;
            }

            if ("open".equalsIgnoreCase(args[0])) {
                return handleOpen(sender, args);
            }
            if ("reload".equalsIgnoreCase(args[0])) {
                return handleReload(sender);
            }
            if ("xp".equalsIgnoreCase(args[0])) {
                return handleXp(sender, args);
            }
            if ("premium".equalsIgnoreCase(args[0])) {
                return handlePremium(sender, args);
            }
            if ("reroll".equalsIgnoreCase(args[0])) {
                return handleReroll(sender, args);
            }
            if ("progress".equalsIgnoreCase(args[0])) {
                return handleProgress(sender, args);
            }
        } catch (Exception exception) {
            sender.sendMessage(placeholders.resolveMessage("&cSopBattlePass error: " + exception.getMessage()));
            exception.printStackTrace();
            return true;
        }

        for (String line : plugin.getConfig().getStringList("messages.usage")) {
            sender.sendMessage(placeholders.resolveMessage(line));
        }
        return true;
    }

    private boolean handleOpen(CommandSender sender, String[] args) throws SQLException {
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.player-only")));
                return true;
            }
            menuService.openMain((Player) sender);
            return true;
        }
        if (!hasAdminPermission(sender)) {
            sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }
        menuService.openMain(target);
        sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.open-other").replace("{player}", target.getName())));
        return true;
    }

    private boolean handleReload(CommandSender sender) throws Exception {
        if (!hasAdminPermission(sender)) {
            sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }
        plugin.reloadPlugin();
        sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.reload")));
        return true;
    }

    private boolean handleXp(CommandSender sender, String[] args) throws SQLException {
        if (!hasAdminPermission(sender)) {
            sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }
        if (args.length < 4 || !"add".equalsIgnoreCase(args[1])) {
            return false;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        int amount = Integer.parseInt(args[3]);
        playerProgressService.addXp(target, amount);
        sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.xp-added")
                .replace("{player}", target.getName() == null ? args[2] : target.getName())
                .replace("{amount}", Integer.toString(amount))));
        return true;
    }

    private boolean handlePremium(CommandSender sender, String[] args) throws SQLException {
        if (!hasAdminPermission(sender)) {
            sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }
        if (args.length < 3) {
            return false;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        boolean grant = "grant".equalsIgnoreCase(args[1]);
        playerProgressService.setPremium(target, grant);
        String path = grant ? "messages.premium-granted" : "messages.premium-revoked";
        sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString(path).replace("{player}", target.getName() == null ? args[2] : target.getName())));
        return true;
    }

    private boolean handleReroll(CommandSender sender, String[] args) throws SQLException {
        if (!hasAdminPermission(sender)) {
            sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }
        if (args.length < 2) {
            return false;
        }
        String missionType = args[1].toLowerCase();
        missionService.reroll("daily".equalsIgnoreCase(missionType) ? MissionType.DAILY : MissionType.WEEKLY);
        String path = "daily".equals(missionType) ? "messages.daily-rerolled" : "messages.weekly-rerolled";
        sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString(path)));
        return true;
    }

    private boolean handleProgress(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }
        if (args.length < 4 || !"custom".equalsIgnoreCase(args[1])) {
            return false;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }
        String customKey = args[3];
        int amount = args.length >= 5 ? Integer.parseInt(args[4]) : 1;
        missionService.recordCustomProgress(target, customKey, amount);
        sender.sendMessage(placeholders.resolveMessage(plugin.getConfig().getString("messages.custom-progress-added")
                .replace("{player}", target.getName())
                .replace("{target}", customKey)
                .replace("{amount}", Integer.toString(amount))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("open", "reload", "xp", "premium", "reroll", "progress"), args[0]);
        }
        if (args.length == 2 && "xp".equalsIgnoreCase(args[0])) {
            return filter(Collections.singletonList("add"), args[1]);
        }
        if (args.length == 2 && "premium".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("grant", "revoke"), args[1]);
        }
        if (args.length == 2 && "reroll".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("daily", "weekly"), args[1]);
        }
        if (args.length == 2 && "progress".equalsIgnoreCase(args[0])) {
            return filter(Collections.singletonList("custom"), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> candidates, String input) {
        List<String> result = new ArrayList<String>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(candidate);
            }
        }
        return result;
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return sender.hasPermission("sopbattlepass.admin");
    }
}
