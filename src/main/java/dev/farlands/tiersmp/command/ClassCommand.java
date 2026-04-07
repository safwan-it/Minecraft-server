package dev.farlands.tiersmp.command;

import dev.farlands.tiersmp.TierService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ClassCommand implements CommandExecutor, TabCompleter {

    public static final String CLASS_GUI_TITLE = ChatColor.DARK_PURPLE + "Choose Your Class";

    private final TierService tierService;

    public ClassCommand(TierService tierService) {
        this.tierService = tierService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatColor.GOLD + "Available classes: " + String.join(", ", tierService.getAllowedClasses()));
            sender.sendMessage(ChatColor.YELLOW + "Use /class choose <id> or /class gui.");
            return true;
        }

        if (args[0].equalsIgnoreCase("gui")) {
            openClassGui(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("choose")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /class choose <id>");
                return true;
            }

            String classId = args[1].toLowerCase();
            if (!tierService.chooseClass(player, classId)) {
                sender.sendMessage(ChatColor.RED + "Unknown class. Use /class list.");
                return true;
            }

            sender.sendMessage(ChatColor.GREEN + "Class selected: " + classId);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /class <list|gui|choose>");
        return true;
    }

    public void openClassGui(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, CLASS_GUI_TITLE);

        int slot = 11;
        for (String classId : tierService.getAllowedClasses()) {
            ItemStack item = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + classId);
                meta.setLore(List.of(ChatColor.GRAY + "Click to choose " + classId));
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            slot += 2;
            if (slot >= 16) {
                break;
            }
        }

        player.openInventory(inventory);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                 @NotNull Command command,
                                                 @NotNull String alias,
                                                 @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("list", "gui", "choose");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("choose")) {
            return new ArrayList<>(tierService.getAllowedClasses());
        }

        return List.of();
    }
}
