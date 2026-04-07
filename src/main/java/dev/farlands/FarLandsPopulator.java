package dev.farlands;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;

import java.util.Random;

public final class FarLandsPopulator implements Listener {

    private final FarLandsPlugin plugin;

    public FarLandsPopulator(FarLandsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChunkPopulate(ChunkPopulateEvent event) {
        if (!plugin.isFarLandsEnabled()) {
            return;
        }

        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        if (!plugin.isWorldAllowed(world.getName())) {
            return;
        }

        int chunkBaseX = chunk.getX() << 4;
        int chunkBaseZ = chunk.getZ() << 4;
        int threshold = plugin.getThreshold();

        if (Math.max(Math.abs(chunkBaseX), Math.abs(chunkBaseZ)) < threshold) {
            return;
        }

        long seed = world.getSeed() ^ (((long) chunk.getX()) << 32) ^ chunk.getZ() ^ 0x5F3759DFL;
        Random random = new Random(seed);

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunkBaseX + localX;
                int worldZ = chunkBaseZ + localZ;

                int distance = Math.max(Math.abs(worldX), Math.abs(worldZ));
                double overDistance = Math.max(0, distance - threshold);
                double distanceFactor = 1.0D + (overDistance / 750_000.0D) * plugin.getIntensity();

                int highestY = world.getHighestBlockYAt(worldX, worldZ);
                Block surface = world.getBlockAt(worldX, highestY, worldZ);
                if (surface.getType().isAir() || surface.isLiquid()) {
                    continue;
                }

                buildPillar(world, random, worldX, worldZ, highestY, surface.getType(), distanceFactor);
                carveSurface(world, random, worldX, worldZ, highestY, distanceFactor);
            }
        }
    }

    private void buildPillar(World world, Random random, int x, int z, int highestY, Material surfaceType, double factor) {
        int maxHeight = Math.min(plugin.getMaxPillarHeight(), world.getMaxHeight() - highestY - 1);
        if (maxHeight <= 0) {
            return;
        }

        int pillarHeight = (int) Math.min(maxHeight, random.nextDouble() * random.nextDouble() * (18 * factor));
        if (pillarHeight <= 0) {
            return;
        }

        Material pillarMaterial = pickPillarMaterial(surfaceType);
        for (int yOffset = 1; yOffset <= pillarHeight; yOffset++) {
            world.getBlockAt(x, highestY + yOffset, z).setType(pillarMaterial, false);
        }
    }

    private void carveSurface(World world, Random random, int x, int z, int highestY, double factor) {
        double carveChance = Math.min(0.90D, plugin.getBaseCarveChance() * factor);
        if (random.nextDouble() >= carveChance) {
            return;
        }

        int maxDepth = Math.max(2, (int) Math.round(9 * factor));
        int carveDepth = 2 + random.nextInt(maxDepth);
        for (int i = 0; i < carveDepth; i++) {
            int y = highestY - i;
            if (y <= world.getMinHeight()) {
                break;
            }
            world.getBlockAt(x, y, z).setType(Material.AIR, false);
        }
    }

    private Material pickPillarMaterial(Material base) {
        if (base == Material.SAND || base == Material.RED_SAND || base == Material.SANDSTONE || base == Material.RED_SANDSTONE) {
            return base;
        }
        if (base == Material.SNOW || base == Material.SNOW_BLOCK || base == Material.ICE) {
            return Material.PACKED_ICE;
        }
        if (base == Material.GRASS_BLOCK || base == Material.DIRT || base == Material.COARSE_DIRT || base == Material.PODZOL) {
            return Material.STONE;
        }
        return base.isSolid() ? base : Material.STONE;
    }
}
