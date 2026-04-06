package dev.farlands.tiersmp;

import dev.farlands.FarLandsPlugin;
import dev.farlands.tiersmp.model.TierProfile;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class TierService {

    public static final String LIFE_CRYSTAL_NAME = ChatColor.LIGHT_PURPLE + "Life Crystal";

    private final FarLandsPlugin plugin;
    private final Map<UUID, TierProfile> profiles = new HashMap<>();
    private final Set<String> allowedClasses = new TreeSet<>();
    private final File dataFolder;
    private final NamespacedKey lifeCrystalKey;

    private int maxTier;
    private int killsRequired;
    private int questRequired;
    private boolean rankDownOnPvpDeath;
    private int maxLives;
    private int maxLifeCrystalCrafts;

    public TierService(FarLandsPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "tiers");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.lifeCrystalKey = new NamespacedKey(plugin, "life_crystal");
        reloadSettings();
    }

    public void reloadSettings() {
        maxTier = Math.max(5, plugin.getConfig().getInt("tiersmp.max-tier", 5));
        killsRequired = Math.max(1, plugin.getConfig().getInt("tiersmp.rankup.required-kills", 3));
        questRequired = Math.max(1, plugin.getConfig().getInt("tiersmp.rankup.required-quest-progress", 20));
        rankDownOnPvpDeath = plugin.getConfig().getBoolean("tiersmp.rankdown.on-player-death", true);
        maxLives = Math.max(1, plugin.getConfig().getInt("tiersmp.lives.max-lives", 3));
        maxLifeCrystalCrafts = Math.max(1, plugin.getConfig().getInt("tiersmp.lives.max-crafts-per-player", 3));

        allowedClasses.clear();
        ConfigurationSection classes = plugin.getConfig().getConfigurationSection("tiersmp.classes");
        if (classes != null) {
            for (String id : classes.getKeys(false)) {
                allowedClasses.add(id.toLowerCase(Locale.ROOT));
            }
        }
    }

    public TierProfile getProfile(UUID playerId) {
        return profiles.computeIfAbsent(playerId, this::loadProfile);
    }

    public void addQuestProgress(Player player, int amount) {
        TierProfile profile = getProfile(player.getUniqueId());
        if (profile.isDead() || profile.isQuestCompleted() || profile.getTier() >= maxTier) {
            return;
        }

        int next = profile.getQuestProgress() + amount;
        profile.setQuestProgress(next);
        if (next >= questRequired) {
            profile.setQuestCompleted(true);
            player.sendMessage(ChatColor.GREEN + "Tier quest completed! Now get kills to rank up.");
        }

        tryRankUp(player, profile);
        saveProfile(profile);
    }

    public void addKill(Player killer) {
        TierProfile profile = getProfile(killer.getUniqueId());
        if (profile.isDead() || profile.getTier() >= maxTier) {
            return;
        }

        profile.setKills(profile.getKills() + 1);
        killer.sendMessage(ChatColor.YELLOW + "Tier kill progress: " + profile.getKills() + "/" + killsRequired);
        tryRankUp(killer, profile);
        saveProfile(profile);
    }

    public void handlePvpDeath(Player victim) {
        TierProfile profile = getProfile(victim.getUniqueId());
        if (profile.isDead() || !rankDownOnPvpDeath) {
            return;
        }

        if (profile.getTier() > 1) {
            profile.setTier(profile.getTier() - 1);
            profile.resetForNextTier();
            victim.sendMessage(ChatColor.RED + "You died in PvP and dropped to Tier " + profile.getTier() + ".");
        } else {
            profile.setLives(profile.getLives() - 1);
            victim.sendMessage(ChatColor.RED + "You lost a life. Lives left: " + profile.getLives());
            if (profile.getLives() <= 0) {
                eliminate(victim, profile);
            }
        }

        saveProfile(profile);
    }

    private void eliminate(Player player, TierProfile profile) {
        profile.setTier(1);
        profile.setLives(0);
        profile.setDead(true);
        player.setGameMode(GameMode.SPECTATOR);
        Bukkit.broadcastMessage(ChatColor.DARK_RED + player.getName() + " has been eliminated. Revive at a beacon with /revive " + player.getName());
    }

    public boolean chooseClass(Player player, String classId) {
        String normalized = classId.toLowerCase(Locale.ROOT);
        if (!allowedClasses.contains(normalized)) {
            return false;
        }
        TierProfile profile = getProfile(player.getUniqueId());
        profile.setSelectedClass(normalized);
        saveProfile(profile);
        return true;
    }

    public void enforcePlayerState(Player player) {
        TierProfile profile = getProfile(player.getUniqueId());
        if (profile.isDead()) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED + "You are eliminated. Wait for a beacon revive.");
        }
    }

    public boolean revive(Player reviver, Player target) {
        TierProfile targetProfile = getProfile(target.getUniqueId());
        if (!targetProfile.isDead()) {
            reviver.sendMessage(ChatColor.RED + "That player is not eliminated.");
            return false;
        }

        if (!isBeaconNearby(reviver.getLocation())) {
            reviver.sendMessage(ChatColor.RED + "Stand near a beacon to revive players.");
            return false;
        }

        targetProfile.setDead(false);
        targetProfile.setLives(1);
        targetProfile.setTier(1);
        targetProfile.resetForNextTier();
        saveProfile(targetProfile);

        target.setGameMode(GameMode.SURVIVAL);
        target.teleport(reviver.getLocation());
        target.sendMessage(ChatColor.GREEN + "You were revived at the beacon by " + reviver.getName() + "!");
        reviver.sendMessage(ChatColor.GREEN + "You revived " + target.getName() + ".");
        return true;
    }

    private boolean isBeaconNearby(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();
        for (int x = baseX - 5; x <= baseX + 5; x++) {
            for (int y = baseY - 4; y <= baseY + 4; y++) {
                for (int z = baseZ - 5; z <= baseZ + 5; z++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.BEACON) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean tryConsumeLifeCrystal(Player player) {
        TierProfile profile = getProfile(player.getUniqueId());
        if (profile.isDead()) {
            player.sendMessage(ChatColor.RED + "Eliminated players cannot use life crystals.");
            return false;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isLifeCrystal(held)) {
            return false;
        }

        if (profile.getLives() >= maxLives) {
            player.sendMessage(ChatColor.YELLOW + "You already have max lives.");
            return true;
        }

        profile.setLives(profile.getLives() + 1);
        int newAmount = held.getAmount() - 1;
        held.setAmount(Math.max(0, newAmount));
        player.sendMessage(ChatColor.GREEN + "Life crystal used. Lives: " + profile.getLives() + "/" + maxLives);
        saveProfile(profile);
        return true;
    }

    public ItemStack createLifeCrystal() {
        ItemStack crystal = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = crystal.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(LIFE_CRYSTAL_NAME);
            meta.getPersistentDataContainer().set(lifeCrystalKey, PersistentDataType.INTEGER, 1);
            crystal.setItemMeta(meta);
        }
        return crystal;
    }

    public boolean isLifeCrystal(ItemStack stack) {
        if (stack == null || stack.getType() != Material.AMETHYST_SHARD || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        Integer marker = meta.getPersistentDataContainer().get(lifeCrystalKey, PersistentDataType.INTEGER);
        return marker != null && marker == 1;
    }

    public boolean canCraftLifeCrystal(Player player) {
        TierProfile profile = getProfile(player.getUniqueId());
        return profile.getCraftedLifeCrystals() < maxLifeCrystalCrafts;
    }

    public void onLifeCrystalCrafted(Player player) {
        TierProfile profile = getProfile(player.getUniqueId());
        profile.setCraftedLifeCrystals(profile.getCraftedLifeCrystals() + 1);
        saveProfile(profile);
    }

    public Set<String> getAllowedClasses() {
        return Collections.unmodifiableSet(allowedClasses);
    }

    public int getKillsRequired() {
        return killsRequired;
    }

    public int getQuestRequired() {
        return questRequired;
    }

    public int getMaxLives() {
        return maxLives;
    }

    public int getMaxLifeCrystalCrafts() {
        return maxLifeCrystalCrafts;
    }

    private void tryRankUp(Player player, TierProfile profile) {
        if (profile.getTier() >= maxTier) {
            return;
        }

        if (profile.getKills() < killsRequired || !profile.isQuestCompleted()) {
            return;
        }

        int nextTier = profile.getTier() + 1;
        if (nextTier >= 5 && (profile.getSelectedClass() == null || profile.getSelectedClass().isBlank())) {
            player.sendMessage(ChatColor.RED + "Choose a class with /class choose <id> or /class gui before unlocking Tier 5.");
            return;
        }

        profile.setTier(nextTier);
        profile.resetForNextTier();
        player.sendMessage(ChatColor.AQUA + "You ranked up to Tier " + nextTier + "!");
    }

    private TierProfile loadProfile(UUID playerId) {
        File file = profileFile(playerId);
        TierProfile profile = new TierProfile(playerId);
        if (!file.exists()) {
            return profile;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        profile.setTier(Math.max(1, yaml.getInt("tier", 1)));
        profile.setKills(Math.max(0, yaml.getInt("kills", 0)));
        profile.setQuestProgress(Math.max(0, yaml.getInt("quest-progress", 0)));
        profile.setQuestCompleted(yaml.getBoolean("quest-completed", false));
        profile.setSelectedClass(yaml.getString("class", null));
        profile.setLives(Math.max(0, yaml.getInt("lives", maxLives)));
        profile.setCraftedLifeCrystals(Math.max(0, yaml.getInt("crafted-life-crystals", 0)));
        profile.setDead(yaml.getBoolean("dead", false));
        return profile;
    }

    public void saveAll() {
        for (TierProfile profile : profiles.values()) {
            saveProfile(profile);
        }
    }

    public void saveProfile(TierProfile profile) {
        File file = profileFile(profile.getPlayerId());
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("tier", profile.getTier());
        yaml.set("kills", profile.getKills());
        yaml.set("quest-progress", profile.getQuestProgress());
        yaml.set("quest-completed", profile.isQuestCompleted());
        yaml.set("class", profile.getSelectedClass());
        yaml.set("lives", profile.getLives());
        yaml.set("crafted-life-crystals", profile.getCraftedLifeCrystals());
        yaml.set("dead", profile.isDead());

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save tier profile for " + profile.getPlayerId() + ": " + exception.getMessage());
        }
    }

    private File profileFile(UUID playerId) {
        return new File(dataFolder, playerId + ".yml");
    }
}
