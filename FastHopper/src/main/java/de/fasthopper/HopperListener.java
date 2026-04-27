package de.fasthopper;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class HopperListener implements Listener {

    private final FastHopper plugin;
    private final Map<String, Long> lastTransferTick = new HashMap<>();
    private BukkitTask cleanupTask;

    // Alle 4 Seiten die ein Comparator neben einem Hopper haben kann
    private static final BlockFace[] FACES = {
        BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    public HopperListener(FastHopper plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        if (!(source.getHolder() instanceof Hopper hopper)) return;

        Block block = hopper.getBlock();

        // Prüfen ob ein Comparator neben diesem Hopper steht
        if (hasSorterComparator(block)) {
            // Sorter-Hopper: Vanilla-Verhalten (1 Item, normales Timing)
            // Nichts tun → Minecraft macht es wie gewohnt
            return;
        }

        // Normaler Hopper: schnell + mehrere Items pro Transfer
        int itemsPerTransfer = plugin.getHopperInterval();

        // 1 Item wird sowieso von Vanilla transferiert.
        // Wir transferieren (itemsPerTransfer - 1) zusätzliche Items sofort danach.
        if (itemsPerTransfer > 1) {
            Inventory destination = event.getDestination();
            // Zusätzliche Items direkt übertragen
            int extra = itemsPerTransfer - 1;
            for (int i = 0; i < extra; i++) {
                if (!transferOneItem(source, destination)) break;
            }
        }

        // Tick-Cooldown auf 1 setzen (maximale Geschwindigkeit)
        String key = locationKey(block);
        long currentTick = getCurrentTick();
        Long last = lastTransferTick.get(key);
        if (last != null && (currentTick - last) < 1) {
            event.setCancelled(true);
            return;
        }
        lastTransferTick.put(key, currentTick);
    }

    /**
     * Prüft ob neben dem Hopper ein Comparator steht (= Sorter-Hopper).
     */
    private boolean hasSorterComparator(Block hopperBlock) {
        for (BlockFace face : FACES) {
            Block neighbor = hopperBlock.getRelative(face);
            if (neighbor.getType() == Material.COMPARATOR) {
                return true;
            }
        }
        return false;
    }

    /**
     * Transferiert ein einzelnes Item von source nach destination.
     * Gibt true zurück wenn erfolgreich.
     */
    private boolean transferOneItem(Inventory source, Inventory destination) {
        for (int i = 0; i < source.getSize(); i++) {
            ItemStack item = source.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            ItemStack single = item.clone();
            single.setAmount(1);

            Map<Integer, ItemStack> leftover = destination.addItem(single);
            if (leftover.isEmpty()) {
                if (item.getAmount() == 1) {
                    source.setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                    source.setItem(i, item);
                }
                return true;
            }
        }
        return false;
    }

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
