package net.enelson.sopbattlepass.mission;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;

public final class MissionListener implements Listener {

    private final MissionService missionService;

    public MissionListener(MissionService missionService) {
        this.missionService = missionService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        missionService.recordSimpleProgress(event.getPlayer(), MissionTriggerType.LOGIN, "", 1);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        missionService.recordSimpleProgress(event.getPlayer(), MissionTriggerType.BLOCK_BREAK, event.getBlock().getType().name(), 1);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }
        missionService.recordSimpleProgress(event.getPlayer(), MissionTriggerType.BLOCK_PLACE, event.getBlock().getType().name(), 1);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        missionService.recordSimpleProgress(killer, MissionTriggerType.ENTITY_KILL, event.getEntityType().name(), 1);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        missionService.recordSimpleProgress(event.getPlayer(), MissionTriggerType.CONSUME, item.getType().name(), item.getAmount());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        missionService.recordSimpleProgress((Player) event.getWhoClicked(), MissionTriggerType.CRAFT, item.getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmeltExtract(FurnaceExtractEvent event) {
        if (event.getItemType() == null || event.getItemType() == Material.AIR) {
            return;
        }
        missionService.recordSimpleProgress(event.getPlayer(), MissionTriggerType.SMELT, event.getItemType().name(), event.getItemAmount());
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Entity caught = event.getCaught();
        if (!(caught instanceof Item)) {
            return;
        }
        ItemStack item = ((Item) caught).getItemStack();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        missionService.recordSimpleProgress(event.getPlayer(), MissionTriggerType.FISH, item.getType().name(), item.getAmount());
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent event) {
        missionService.recordSimpleProgress(event.getPlayer(), MissionTriggerType.SHEAR, event.getEntity().getType().name(), 1);
    }

    @EventHandler
    public void onMilk(PlayerBucketFillEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        ItemStack result = event.getItemStack();
        if (result == null || result.getType() != Material.MILK_BUCKET) {
            return;
        }
        missionService.recordSimpleProgress(event.getPlayer(), MissionTriggerType.MILK, Material.MILK_BUCKET.name(), 1);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK:
                missionService.recordSimpleProgress(event.getPlayer(), MissionTriggerType.RIGHT_CLICK_BLOCK, event.getClickedBlock().getType().name(), 1);
                break;
            default:
                break;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        int amount = Math.max(1, (int) Math.ceil(event.getFinalDamage()));
        missionService.recordSimpleProgress((Player) event.getDamager(), MissionTriggerType.DAMAGE_PLAYER, "PLAYER", amount);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom() == null || event.getTo() == null) {
            return;
        }
        double distance = event.getFrom().distance(event.getTo());
        if (distance <= 0.0D) {
            return;
        }
        int amount = (int) Math.floor(distance);
        if (amount <= 0) {
            return;
        }
        Player player = event.getPlayer();
        missionService.recordSimpleProgress(player, MissionTriggerType.MOVE, "", amount);
        if (player.isSneaking()) {
            missionService.recordSimpleProgress(player, MissionTriggerType.SNEAK_MOVE, "", amount);
        }
        if (player.isSprinting()) {
            missionService.recordSimpleProgress(player, MissionTriggerType.SPRINT, "", amount);
        }
        if (player.isSwimming() || player.getLocation().getBlock().isLiquid()) {
            missionService.recordSimpleProgress(player, MissionTriggerType.SWIM, "", amount);
        }
    }
}
