package dev.farlands.tiersmp.listener;

import dev.farlands.tiersmp.TierService;
import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public final class TierQuestListener implements Listener {

    private final TierService tierService;

    public TierQuestListener(TierService tierService) {
        this.tierService = tierService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getCaught() instanceof org.bukkit.entity.Item) {
            tierService.onFishCatch(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDiamondMine(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        if (type == Material.DIAMOND_ORE || type == Material.DEEPSLATE_DIAMOND_ORE) {
            tierService.onDiamondMined(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorChange(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (hasFullDiamond(player)) {
            tierService.onFullDiamondArmor(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (hasFullDiamond(event.getPlayer())) {
            tierService.onFullDiamondArmor(event.getPlayer());
        }
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }

        Player killer = dragon.getKiller();
        if (killer != null) {
            tierService.onDragonKill(killer);
        }
    }

    private boolean hasFullDiamond(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        return armor.length == 4
                && armor[0] != null && armor[0].getType() == Material.DIAMOND_BOOTS
                && armor[1] != null && armor[1].getType() == Material.DIAMOND_LEGGINGS
                && armor[2] != null && armor[2].getType() == Material.DIAMOND_CHESTPLATE
                && armor[3] != null && armor[3].getType() == Material.DIAMOND_HELMET;
    }
}
