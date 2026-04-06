package dev.farlands.tiersmp.command;

import dev.farlands.tiersmp.TierService;
import dev.farlands.tiersmp.model.TierProfile;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TierCommand implements CommandExecutor {

    private final TierService tierService;

    public TierCommand(TierService tierService) {
        this.tierService = tierService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        TierProfile profile = tierService.getProfile(player.getUniqueId());
        sender.sendMessage(ChatColor.GOLD + "Tier: " + profile.getTier());
        sender.sendMessage(ChatColor.YELLOW + "Kills: " + profile.getKills() + "/" + tierService.getKillsRequired());
        sender.sendMessage(ChatColor.GREEN + "Quest: " + profile.getQuestProgress() + "/" + tierService.getQuestRequired());
        sender.sendMessage(ChatColor.AQUA + "Class: " + (profile.getSelectedClass() == null ? "not selected" : profile.getSelectedClass()));
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Lives: " + profile.getLives() + "/" + tierService.getMaxLives());
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Life crystals crafted: " + profile.getCraftedLifeCrystals() + "/" + tierService.getMaxLifeCrystalCrafts());
        sender.sendMessage(ChatColor.RED + "Eliminated: " + profile.isDead());
        return true;
    }
}
