package dev.farlands;

import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FarLandsBossEventListener implements Listener {

    private final FarLandsPlugin plugin;
    private final Set<UUID> activeBosses = new HashSet<>();
    private long lastSpawnAtMillis;

    public FarLandsBossEventListener(FarLandsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || !movedBlock(event)) {
            return;
        }

        Player player = event.getPlayer();
        World world = player.getWorld();

        if (!plugin.isFarLandsEnabled() || !plugin.isWorldAllowed(world.getName())) {
            return;
        }

        int threshold = plugin.getThreshold();
        int distance = Math.max(Math.abs(player.getLocation().getBlockX()), Math.abs(player.getLocation().getBlockZ()));
        if (distance < threshold) {
            return;
        }

        if (!plugin.getConfig().getBoolean("farlands.boss-event.enabled", true)) {
            return;
        }

        if (!activeBosses.isEmpty()) {
            return;
        }

        long cooldownMs = plugin.getConfig().getLong("farlands.boss-event.spawn-cooldown-seconds", 900) * 1000L;
        if ((System.currentTimeMillis() - lastSpawnAtMillis) < cooldownMs) {
            return;
        }

        spawnBossNear(player);
    }

    private boolean movedBlock(PlayerMoveEvent event) {
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }

    private void spawnBossNear(Player player) {
        World world = player.getWorld();
        Location base = player.getLocation().clone().add(8, 0, 8);
        int y = Math.max(world.getHighestBlockYAt(base), world.getSeaLevel() + 2);
        Location spawn = new Location(world, base.getX(), y, base.getZ());

        Warden boss = world.spawn(spawn, Warden.class, entity -> {
            entity.setCustomName(ChatColor.DARK_PURPLE + "Far Lands Aberration");
            entity.setCustomNameVisible(true);
            entity.setHealth(Math.min(entity.getMaxHealth(), 250.0));
            entity.setPersistent(true);
            entity.setRemoveWhenFarAway(false);
        });

        activeBosses.add(boss.getUniqueId());
        lastSpawnAtMillis = System.currentTimeMillis();

        Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "A Far Lands boss has appeared at "
                + ChatColor.LIGHT_PURPLE + world.getName() + " " + spawn.getBlockX() + " " + spawn.getBlockY() + " " + spawn.getBlockZ());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!activeBosses.remove(entity.getUniqueId())) {
            return;
        }

        event.getDrops().clear();
        event.getDrops().add(new ItemStack(Material.DIAMOND, plugin.getConfig().getInt("farlands.boss-event.loot.diamonds", 8)));
        event.getDrops().add(new ItemStack(Material.NETHERITE_SCRAP, plugin.getConfig().getInt("farlands.boss-event.loot.netherite-scrap", 2)));
        event.getDrops().add(new ItemStack(Material.ECHO_SHARD, plugin.getConfig().getInt("farlands.boss-event.loot.echo-shards", 6)));
        event.getDrops().add(plugin.getTierService().createLifeCrystal());

        Player killer = entity.getKiller();
        if (killer != null) {
            plugin.getTierService().onFarLandsBossKill(killer);
        }

        Bukkit.broadcastMessage(ChatColor.GOLD + "The Far Lands Aberration was defeated! Loot dropped at its death location.");
    }
}
