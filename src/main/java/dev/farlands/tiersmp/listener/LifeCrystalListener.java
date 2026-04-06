package dev.farlands.tiersmp.listener;

import dev.farlands.tiersmp.TierService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public final class LifeCrystalListener implements Listener {

    private final TierService tierService;

    public LifeCrystalListener(TierService tierService) {
        this.tierService = tierService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (tierService.tryConsumeLifeCrystal(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null || recipe.getResult().getType() != Material.AMETHYST_SHARD) {
            return;
        }

        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        if (!tierService.canCraftLifeCrystal(player)) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
        } else {
            ItemStack result = tierService.createLifeCrystal();
            event.getInventory().setResult(result);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (!tierService.isLifeCrystal(current)) {
            return;
        }

        if (!tierService.canCraftLifeCrystal(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You already crafted the max number of life crystals.");
            return;
        }

        tierService.onLifeCrystalCrafted(player);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Life Crystal crafted (" + tierService.getMaxLifeCrystalCrafts() + " max crafts per player).");
    }
}
