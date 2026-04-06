package dev.farlands.tiersmp.listener;

import dev.farlands.tiersmp.TierService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerStateListener implements Listener {

    private final TierService tierService;

    public PlayerStateListener(TierService tierService) {
        this.tierService = tierService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        tierService.enforcePlayerState(event.getPlayer());
    }
}
