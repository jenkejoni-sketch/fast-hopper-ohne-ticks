package de.fasthopper;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class HopperListener implements Listener {

    private final FastHopper plugin;

    // Speichert den letzten Tick, in dem ein Hopper ein Item transferiert hat.
    // Key = "welt:x:y:z"
    private final Map<String, Long> lastTransferTick = new HashMap<>();

    private BukkitTask cleanupTask;

    public HopperListener(FastHopper plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    // -----------------------------------------------------------------------
    // Haupt-Event: jeden Hopper-Transfer abfangen
    // -----------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();

        // Nur Hopper als Quelle behandeln
        if (!(source.getHolder() instanceof Hopper hopper)) return;

        int cooldown = getCooldownTicks();

        // Bei Vanilla-Speed (8 Ticks) nichts tun
        if (cooldown >= 8) return;

        String key = locationKey(hopper.getBlock());
        long currentTick = getCurrentTick();

        Long last = lastTransferTick.get(key);

        if (last != null && (currentTick - last) < cooldown) {
            // Cooldown noch nicht abgelaufen → Transfer blockieren
            // Minecraft versucht es beim nächsten Tick automatisch erneut
            event.setCancelled(true);
            return;
        }

        // Transfer erlauben und Tick speichern
        // → Es wird IMMER nur 1 Item transferiert (Vanilla-Verhalten bleibt)
        // → Item-Sorter funktionieren korrekt weiter!
        lastTransferTick.put(key, currentTick);
    }

    // -----------------------------------------------------------------------
    // Aufräumen: alte Einträge alle 30 Sekunden löschen
    // -----------------------------------------------------------------------

    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTick = getCurrentTick();
            lastTransferTick.entrySet().removeIf(e -> (currentTick - e.getValue()) > 200);
        }, 600L, 600L);
    }

    public void shutdown() {
        if (cleanupTask != null) cleanupTask.cancel();
        lastTransferTick.clear();
    }

    // -----------------------------------------------------------------------
    // Hilfsmethoden
    // -----------------------------------------------------------------------

    /**
     * Berechnet den Cooldown in Ticks zwischen zwei Transfers.
     *
     * Vanilla-Hopper transferieren 1 Item alle 8 Ticks.
     * Unser "hopper-interval" ist ein Geschwindigkeits-Multiplikator:
     *
     *   interval 1  → 8 Ticks Pause (= Vanilla)
     *   interval 2  → 4 Ticks Pause (2× schneller)
     *   interval 4  → 2 Ticks Pause (4× schneller)
     *   interval 8  → 1 Tick  Pause (8× schneller, Maximum)
     *
     * Immer nur 1 Item pro Transfer → Item-Sorter bleiben kompatibel!
     */
    private int getCooldownTicks() {
        int multiplier = plugin.getHopperInterval();
        return Math.max(1, 8 / multiplier);
    }

    private long getCurrentTick() {
        for (World world : Bukkit.getWorlds()) {
            return world.getFullTime();
        }
        return 0;
    }

    private String locationKey(Block block) {
        return block.getWorld().getName()
                + ":" + block.getX()
                + ":" + block.getY()
                + ":" + block.getZ();
    }
}
