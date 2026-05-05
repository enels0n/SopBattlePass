package net.enelson.sopbattlepass.gui;

import net.enelson.sopbattlepass.menu.MenuView;
import net.enelson.sopbattlepass.reward.RewardTrack;
import net.enelson.sopli.lib.SopLib;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class BattlePassMenuListener implements Listener {

    private final BattlePassMenuService menuService;

    public BattlePassMenuListener(BattlePassMenuService menuService) {
        this.menuService = menuService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!menuService.isManagedInventory(title)) {
            return;
        }

        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();
        if (current == null) {
            return;
        }
        String customKey = SopLib.getInstance().getItemUtils().getCustomItemKey(current);
        if (customKey == null || customKey.isEmpty()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if ("menu:view:DAILY".equalsIgnoreCase(customKey)) {
            menuService.handleViewOpen(player, MenuView.DAILY);
            return;
        }
        if ("menu:view:WEEKLY".equalsIgnoreCase(customKey)) {
            menuService.handleViewOpen(player, MenuView.WEEKLY_WEEKS);
            return;
        }
        if ("menu:view:GLOBAL".equalsIgnoreCase(customKey)) {
            menuService.handleViewOpen(player, MenuView.GLOBAL);
            return;
        }
        if ("menu:view:REWARDS".equalsIgnoreCase(customKey)) {
            menuService.handleViewOpen(player, MenuView.REWARDS);
            return;
        }
        if (customKey.startsWith("menu:claim:")) {
            String[] parts = customKey.split(":");
            if (parts.length >= 4) {
                menuService.handleRewardClaim(player, Integer.parseInt(parts[2]), RewardTrack.valueOf(parts[3]));
            }
            return;
        }
        if (customKey.startsWith("menu:nav:")) {
            String[] parts = customKey.split(":");
            if (parts.length >= 3) {
                menuService.handleRewardPage(player, Integer.parseInt(parts[2]));
            }
            return;
        }
        if (customKey.startsWith("menu:mission_nav:")) {
            String[] parts = customKey.split(":");
            if (parts.length >= 4) {
                menuService.handleMissionPage(player, MenuView.valueOf(parts[2]), Integer.parseInt(parts[3]));
            }
            return;
        }
        if (customKey.startsWith("menu:weekly_weeks_nav:")) {
            String[] parts = customKey.split(":");
            if (parts.length >= 3) {
                menuService.handleWeeklyWeekSelectorPage(player, Integer.parseInt(parts[2]));
            }
            return;
        }
        if (customKey.startsWith("menu:weekly_week:")) {
            String[] parts = customKey.split(":");
            if (parts.length >= 3) {
                menuService.handleWeeklyWeekPage(player, Integer.parseInt(parts[2]), 0);
            }
            return;
        }
        if (customKey.startsWith("menu:weekly_page:")) {
            String[] parts = customKey.split(":");
            if (parts.length >= 4) {
                menuService.handleWeeklyWeekPage(player, Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            }
            return;
        }
        if ("menu:weekly_selector".equalsIgnoreCase(customKey)) {
            menuService.handleViewOpen(player, MenuView.WEEKLY_WEEKS);
            return;
        }
        if ("menu:back".equalsIgnoreCase(customKey)) {
            try {
                menuService.openMain(player);
            } catch (Exception ignored) {
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (!menuService.isManagedInventory(title)) {
            return;
        }
        event.setCancelled(true);
    }
}
