package dev.farlands.tiersmp.listener;

import dev.farlands.tiersmp.TierService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class TierQuestListener implements Listener {

    private final TierService tierService;

    public TierQuestListener(TierService tierService) {
        this.tierService = tierService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        tierService.addQuestProgress(event.getPlayer(), 1);
    }
}
