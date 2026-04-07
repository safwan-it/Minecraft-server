package dev.farlands.tiersmp.model;

import java.util.UUID;

public final class TierProfile {

    private final UUID playerId;
    private int tier;
    private int kills;
    private int questProgress;
    private boolean questCompleted;
    private String selectedClass;
    private int lives;
    private int craftedLifeCrystals;
    private boolean dead;

    public TierProfile(UUID playerId, int startingLives) {
        this.playerId = playerId;
        this.tier = 1;
        this.lives = Math.max(1, startingLives);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getQuestProgress() {
        return questProgress;
    }

    public void setQuestProgress(int questProgress) {
        this.questProgress = questProgress;
    }

    public boolean isQuestCompleted() {
        return questCompleted;
    }

    public void setQuestCompleted(boolean questCompleted) {
        this.questCompleted = questCompleted;
    }

    public String getSelectedClass() {
        return selectedClass;
    }

    public void setSelectedClass(String selectedClass) {
        this.selectedClass = selectedClass;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public int getCraftedLifeCrystals() {
        return craftedLifeCrystals;
    }

    public void setCraftedLifeCrystals(int craftedLifeCrystals) {
        this.craftedLifeCrystals = craftedLifeCrystals;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public void resetForNextTier() {
        this.kills = 0;
        this.questProgress = 0;
        this.questCompleted = false;
    }
}
