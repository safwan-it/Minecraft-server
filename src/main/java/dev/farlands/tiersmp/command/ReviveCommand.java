package dev.farlands.tiersmp.command;

import dev.farlands.tiersmp.TierService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ReviveCommand implements CommandExecutor {

    private final TierService tierService;

    public ReviveCommand(TierService tierService) {
        this.tierService = tierService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /revive <player> (hold a Revival Beacon)");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player must be online to revive.");
            return true;
        }

        tierService.revive(player, target);
        return true;
    }
}
