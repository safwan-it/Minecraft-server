package dev.farlands;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class FarLandsCommand implements CommandExecutor, TabCompleter {

    private final FarLandsPlugin plugin;

    public FarLandsCommand(FarLandsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6FarLands commands:");
            sender.sendMessage("§e/farlands reload §7- reload plugin config");
            sender.sendMessage("§e/farlands tp [x z] §7- teleport to farlands area");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.loadSettings();
            sender.sendMessage("§aFarLands config reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("tp")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can use /farlands tp.");
                return true;
            }

            int x = plugin.getThreshold();
            int z = plugin.getThreshold();

            if (args.length >= 3) {
                try {
                    x = Integer.parseInt(args[1]);
                    z = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    player.sendMessage("§cCoordinates must be whole numbers.");
                    return true;
                }
            }

            World world = player.getWorld();
            int y = Math.max(world.getHighestBlockYAt(x, z) + 2, world.getSeaLevel() + 2);
            player.teleport(new Location(world, x + 0.5, y, z + 0.5));
            player.sendMessage("§aTeleported near Far Lands zone at §f" + x + " " + z + "§a.");
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Use /farlands.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                 @NotNull Command command,
                                                 @NotNull String alias,
                                                 @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            if ("tp".startsWith(args[0].toLowerCase())) {
                completions.add("tp");
            }
            return completions;
        }
        return List.of();
    }
}
