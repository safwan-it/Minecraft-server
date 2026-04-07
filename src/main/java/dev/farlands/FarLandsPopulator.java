package dev.farlands;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;

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

                if (isWallColumn(worldX, worldZ, distanceFactor)) {
                    buildFarLandsWall(world, worldX, worldZ, highestY, surface.getType(), distanceFactor);
                } else {
                    mutateTerrainColumn(world, worldX, worldZ, highestY, surface.getType(), distanceFactor);
                }
            }
        }
    }

    private void mutateTerrainColumn(World world, int x, int z, int highestY, Material surfaceType, double factor) {
        int delta = computeHeightDelta(x, z, factor);
        if (delta > 0) {
            raiseColumn(world, x, z, highestY, surfaceType, delta);
            int topY = Math.min(world.getMaxHeight() - 2, highestY + delta);
            if (shouldCreateShelf(x, z, factor)) {
                createShelf(world, x, z, topY, surfaceType, factor);
            }
            carveSurface(world, x, z, topY, factor);
            return;
        }

        if (delta < 0) {
            int carveDepth = Math.min(Math.abs(delta), 20);
            for (int i = 0; i < carveDepth; i++) {
                int y = highestY - i;
                if (y <= world.getMinHeight()) {
                    break;
                }
                world.getBlockAt(x, y, z).setType(Material.AIR, false);
            }
            carveSurface(world, x, z, highestY, factor);
        }
    }

    private boolean isWallColumn(int x, int z, double factor) {
        double clusterNoise = (blockNoise(x, z, 96) * 0.55D) + (blockNoise(x + 41, z - 63, 48) * 0.45D);
        if (clusterNoise < -0.06D) {
            return false;
        }

        int grid = 20 + (int) Math.round((blockNoise(x + 500, z - 500, 128) + 1.0D) * 5.0D);
        int thickness = 2 + Math.min(4, (int) Math.floor(factor));
        int warpX = (int) Math.round(blockNoise(x - 93, z + 27, 40) * 7.0D);
        int warpZ = (int) Math.round(blockNoise(x + 57, z - 29, 40) * 7.0D);
        int rx = Math.floorMod(x + warpX, grid);
        int rz = Math.floorMod(z + warpZ, grid);
        boolean xWall = rx < thickness || rx >= (grid - thickness);
        boolean zWall = rz < thickness || rz >= (grid - thickness);
        if (!(xWall || zWall)) {
            return false;
        }

        // Keep repeating gaps so the pattern feels glitched/segmented.
        return blockNoise(x + 133, z - 77, 13) > -0.33D;
    }

    private void buildFarLandsWall(World world, int x, int z, int highestY, Material surfaceType, double factor) {
        int maxY = world.getMaxHeight() - 2;
        int minWallTop = Math.max(highestY + 8, maxY - 64);
        int randomDrop = (int) (Math.abs(blockNoise(x, z, 9)) * 22);
        int topY = Math.max(minWallTop, maxY - randomDrop);

        Material fill = pickPillarMaterial(surfaceType);
        for (int y = highestY + 1; y <= topY; y++) {
            world.getBlockAt(x, y, z).setType(fill, false);
        }

        carveWallCaves(world, x, z, topY, factor);
    }

    private void carveWallCaves(World world, int x, int z, int topY, double factor) {
        int minY = Math.max(world.getMinHeight() + 20, topY - (45 + (int) (factor * 6)));
        for (int y = minY; y < topY - 3; y += 2) {
            double caveNoise = blockNoise(x + y, z - y, 8);
            double tunnelNoise = blockNoise((x * 2) - y, (z * 2) + y, 11);
            if (caveNoise > 0.36D || (caveNoise > 0.22D && tunnelNoise > 0.40D)) {
                world.getBlockAt(x, y, z).setType(Material.AIR, false);
                world.getBlockAt(x, y + 1, z).setType(Material.AIR, false);
                if (blockNoise(x + y * 3, z - y * 2, 7) > 0.55D) {
                    world.getBlockAt(x, y - 1, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private void raiseColumn(World world, int x, int z, int highestY, Material surfaceType, int riseHeight) {
        int maxHeight = Math.min(riseHeight, world.getMaxHeight() - highestY - 2);
        if (maxHeight <= 0) {
            return;
        }

        Material fillMaterial = pickPillarMaterial(surfaceType);
        for (int yOffset = 1; yOffset <= maxHeight; yOffset++) {
            world.getBlockAt(x, highestY + yOffset, z).setType(fillMaterial, false);
        }
    }

    private void createShelf(World world, int centerX, int centerZ, int y, Material surfaceType, double factor) {
        int radius = 2 + (int) Math.min(4, Math.floor(factor));
        Material fillMaterial = pickPillarMaterial(surfaceType);
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int manhattan = Math.abs(centerX - x) + Math.abs(centerZ - z);
                if (manhattan > radius + 1) {
                    continue;
                }
                Block block = world.getBlockAt(x, y, z);
                if (block.getType().isAir()) {
                    block.setType(fillMaterial, false);
                }
                Block roof = world.getBlockAt(x, y + 1, z);
                if (!roof.getType().isAir() && hash(x, y, z) % 4 == 0) {
                    roof.setType(Material.AIR, false);
                }
            }
        }
    }

    private int computeHeightDelta(int x, int z, double factor) {
        double waveX = Math.sin(x * 0.013D);
        double waveZ = Math.cos(z * 0.011D);
        double folded = Math.sin((x + z) * 0.0048D);
        folded = Math.copySign(Math.pow(Math.abs(folded), 0.35D), folded);
        double blocky = blockNoise(x, z, 24);

        double combined = (waveX * 0.20D) + (waveZ * 0.18D) + (folded * 0.34D) + (blocky * 0.52D);
        double scaled = combined * (16.0D * factor);
        return (int) Math.round(clamp(scaled, -22.0D, plugin.getMaxPillarHeight()));
    }

    private boolean shouldCreateShelf(int x, int z, double factor) {
        double shelfNoise = blockNoise(x + 71, z - 39, 18);
        double threshold = 0.58D - Math.min(0.18D, factor * 0.03D);
        return shelfNoise > threshold;
    }

    private double blockNoise(int x, int z, int cellSize) {
        int cellX = Math.floorDiv(x, cellSize);
        int cellZ = Math.floorDiv(z, cellSize);
        long hashed = hash(cellX, 0, cellZ);
        return ((hashed & 0xFFFFL) / 32767.5D) - 1.0D;
    }

    private long hash(int x, int y, int z) {
        long value = 1469598103934665603L;
        value ^= x * 0x9E3779B97F4A7C15L;
        value *= 1099511628211L;
        value ^= y * 0xC2B2AE3D27D4EB4FL;
        value *= 1099511628211L;
        value ^= z * 0x165667B19E3779F9L;
        value *= 1099511628211L;
        return value;
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private void carveSurface(World world, int x, int z, int highestY, double factor) {
        double carveChance = Math.min(0.70D, plugin.getBaseCarveChance() * factor * 0.5D);
        if (blockNoise(x - 91, z + 127, 12) < (1.0D - carveChance * 2.0D)) {
            return;
        }

        int maxDepth = Math.max(2, (int) Math.round(6 * factor));
        int carveDepth = 2 + (int) (Math.abs(blockNoise(x + 7, z + 13, 9)) * maxDepth);
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
