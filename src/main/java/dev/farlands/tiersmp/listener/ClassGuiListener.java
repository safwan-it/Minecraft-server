package dev.farlands.tiersmp.listener;

import dev.farlands.tiersmp.TierService;
import dev.farlands.tiersmp.command.ClassCommand;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ClassGuiListener implements Listener {

    private final TierService tierService;

    public ClassGuiListener(TierService tierService) {
        this.tierService = tierService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ClassCommand.CLASS_GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        String displayName = meta.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            return;
        }

        String classId = ChatColor.stripColor(displayName).toLowerCase();
        if (!tierService.chooseClass(player, classId)) {
            player.sendMessage(ChatColor.RED + "Invalid class selection.");
            return;
        }

        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Class selected: " + classId);
    }
}
