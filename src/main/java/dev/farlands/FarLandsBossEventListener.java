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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FarLandsBossEventListener implements Listener {

    private static final String FAR_LANDS_BOSS_MARKER = "farlands_boss_marker";

    private final FarLandsPlugin plugin;
    private final Set<UUID> activeBosses = new HashSet<>();
    private long lastSpawnAtMillis;
    private long nextBossRefreshAtMillis;

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

        long cooldownMs = plugin.getConfig().getLong("farlands.boss-event.spawn-cooldown-seconds", 900) * 1000L;
        long now = System.currentTimeMillis();
        if ((now - lastSpawnAtMillis) < cooldownMs) {
            return;
        }

        if (!activeBosses.isEmpty()) {
            return;
        }
        refreshActiveBosses();
        if (!activeBosses.isEmpty()) {
            return;
        }

        if (now < nextBossRefreshAtMillis) {
            return;
        }
        long refreshIntervalMs = plugin.getConfig().getLong("farlands.boss-event.active-boss-refresh-interval-seconds", 10) * 1000L;
        nextBossRefreshAtMillis = now + Math.max(1_000L, refreshIntervalMs);

        refreshActiveBosses();
        if (!activeBosses.isEmpty()) {
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
        Location playerLocation = player.getLocation();
        Vector deeperFarLandsDirection = new Vector(playerLocation.getX(), 0, playerLocation.getZ());
        if (deeperFarLandsDirection.lengthSquared() < 0.0001D) {
            deeperFarLandsDirection = playerLocation.getDirection().setY(0);
        }
        if (deeperFarLandsDirection.lengthSquared() < 0.0001D) {
            deeperFarLandsDirection = new Vector(1, 0, 0);
        }
        deeperFarLandsDirection.normalize();

        double arenaDistanceFromPlayer = 100.0D;
        double x = playerLocation.getX() + (deeperFarLandsDirection.getX() * arenaDistanceFromPlayer);
        double z = playerLocation.getZ() + (deeperFarLandsDirection.getZ() * arenaDistanceFromPlayer);

        Location spawn = findTopOfFarLands(world, x, z);

        Warden boss = world.spawn(spawn, Warden.class, entity -> {
            entity.setCustomName(ChatColor.DARK_PURPLE + "Far Lands Aberration");
            entity.setCustomNameVisible(true);
            entity.setHealth(Math.min(entity.getMaxHealth(), 250.0));
            entity.setPersistent(true);
            entity.setRemoveWhenFarAway(false);
            markAsFarLandsBoss(entity.getPersistentDataContainer());
        });

        activeBosses.add(boss.getUniqueId());
        lastSpawnAtMillis = System.currentTimeMillis();

        Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "A Far Lands boss has appeared at "
                + ChatColor.LIGHT_PURPLE + world.getName() + " " + spawn.getBlockX() + " " + spawn.getBlockY() + " " + spawn.getBlockZ());
    }

    private Location findTopOfFarLands(World world, double centerX, double centerZ) {
        int baseX = (int) Math.floor(centerX);
        int baseZ = (int) Math.floor(centerZ);
        int bestX = baseX;
        int bestZ = baseZ;
        int highestY = world.getSeaLevel() + 2;

        int scanRadius = 8;
        for (int offsetX = -scanRadius; offsetX <= scanRadius; offsetX++) {
            for (int offsetZ = -scanRadius; offsetZ <= scanRadius; offsetZ++) {
                int checkX = baseX + offsetX;
                int checkZ = baseZ + offsetZ;
                int candidateY = world.getHighestBlockYAt(checkX, checkZ);
                if (candidateY > highestY) {
                    highestY = candidateY;
                    bestX = checkX;
                    bestZ = checkZ;
                }
            }
        }

        return new Location(world, bestX + 0.5D, highestY + 1.0D, bestZ + 0.5D);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        boolean wasTracked = activeBosses.remove(entity.getUniqueId());
        boolean wasMarkedBoss = isMarkedFarLandsBoss(entity.getPersistentDataContainer());
        if (!wasTracked && !wasMarkedBoss) {
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

    private void refreshActiveBosses() {
        activeBosses.clear();
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (isMarkedFarLandsBoss(entity.getPersistentDataContainer())) {
                    activeBosses.add(entity.getUniqueId());
                }
            }
        }
    }

    private void markAsFarLandsBoss(PersistentDataContainer container) {
        container.set(new NamespacedKey(plugin, FAR_LANDS_BOSS_MARKER), PersistentDataType.BYTE, (byte) 1);
    }

    private boolean isMarkedFarLandsBoss(PersistentDataContainer container) {
        Byte marker = container.get(new NamespacedKey(plugin, FAR_LANDS_BOSS_MARKER), PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }
}
