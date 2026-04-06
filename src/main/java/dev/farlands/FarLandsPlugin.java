package dev.farlands;

import dev.farlands.tiersmp.TierService;
import dev.farlands.tiersmp.command.ClassCommand;
import dev.farlands.tiersmp.command.ReviveCommand;
import dev.farlands.tiersmp.command.TierCommand;
import dev.farlands.tiersmp.listener.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FarLandsPlugin extends JavaPlugin {

    private boolean enabled;
    private int threshold;
    private double intensity;
    private int maxPillarHeight;
    private double baseCarveChance;
    private final Set<String> allowedWorlds = new HashSet<>();
    private TierService tierService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new FarLandsPopulator(this), this);
        pluginManager.registerEvents(new FarLandsBossEventListener(this), this);

        FarLandsCommand farLandsCommand = new FarLandsCommand(this);
        if (getCommand("farlands") != null) {
            getCommand("farlands").setExecutor(farLandsCommand);
            getCommand("farlands").setTabCompleter(farLandsCommand);
        }

        tierService = new TierService(this);
        pluginManager.registerEvents(new TierCombatListener(tierService), this);
        pluginManager.registerEvents(new TierQuestListener(tierService), this);
        pluginManager.registerEvents(new ClassGuiListener(tierService), this);
        pluginManager.registerEvents(new PlayerStateListener(tierService), this);
        pluginManager.registerEvents(new LifeCrystalListener(tierService), this);

        if (getCommand("tier") != null) {
            getCommand("tier").setExecutor(new TierCommand(tierService));
        }

        ClassCommand classCommand = new ClassCommand(tierService);
        if (getCommand("class") != null) {
            getCommand("class").setExecutor(classCommand);
            getCommand("class").setTabCompleter(classCommand);
        }

        if (getCommand("revive") != null) {
            getCommand("revive").setExecutor(new ReviveCommand(tierService));
        }

        registerLifeCrystalRecipe();

        getLogger().info("FarLands12111 enabled with TierSMP support.");
    }

    private void registerLifeCrystalRecipe() {
        NamespacedKey key = new NamespacedKey(this, "life_crystal");
        ItemStack result = tierService.createLifeCrystal();
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("AEA", "ENE", "AEA");
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('E', Material.ENDER_PEARL);
        recipe.setIngredient('N', Material.NETHER_STAR);
        Bukkit.addRecipe(recipe);
    }

    @Override
    public void onDisable() {
        if (tierService != null) {
            tierService.saveAll();
        }
    }

    public void loadSettings() {
        reloadConfig();
        FileConfiguration config = getConfig();

        enabled = config.getBoolean("farlands.enabled", true);
        threshold = Math.max(1, config.getInt("farlands.threshold", 12_000_000));
        intensity = Math.max(0.0D, config.getDouble("farlands.intensity", 1.0D));
        maxPillarHeight = Math.max(1, config.getInt("farlands.max-pillar-height", 70));
        baseCarveChance = clamp(config.getDouble("farlands.base-carve-chance", 0.08D), 0.0D, 1.0D);

        allowedWorlds.clear();
        List<String> configuredWorlds = config.getStringList("farlands.worlds");
        if (configuredWorlds.isEmpty()) {
            allowedWorlds.add("world");
        } else {
            for (String worldName : configuredWorlds) {
                if (!worldName.isBlank()) {
                    allowedWorlds.add(worldName.trim());
                }
            }
        }

        if (tierService != null) {
            tierService.reloadSettings();
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public boolean isFarLandsEnabled() {
        return enabled;
    }

    public int getThreshold() {
        return threshold;
    }

    public double getIntensity() {
        return intensity;
    }

    public int getMaxPillarHeight() {
        return maxPillarHeight;
    }

    public double getBaseCarveChance() {
        return baseCarveChance;
    }

    public TierService getTierService() {
        return tierService;
    }

    public boolean isWorldAllowed(String worldName) {
        return allowedWorlds.contains(worldName);
    }
}
