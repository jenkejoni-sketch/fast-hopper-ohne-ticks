package de.fasthopper;

import org.bukkit.plugin.java.JavaPlugin;

public class FastHopper extends JavaPlugin {

    private static FastHopper instance;
    private HopperListener hopperListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info("FastHopper wurde aktiviert!");
        getLogger().info("Hopper-Transfer-Interval: " + getHopperInterval() + " Tick(s)");

        HopperListener hopperListener = new HopperListener(this);
        getServer().getPluginManager().registerEvents(hopperListener, this);
        this.hopperListener = hopperListener;

        FastHopperCommand cmd = new FastHopperCommand(this);
        getCommand("fasthopper").setExecutor(cmd);
        getCommand("fasthopper").setTabCompleter(cmd);
    }

    @Override
    public void onDisable() {
        if (hopperListener != null) hopperListener.shutdown();
        getLogger().info("FastHopper wurde deaktiviert.");
    }

    public int getHopperInterval() {
        int interval = getConfig().getInt("hopper-interval", 1);
        if (interval < 1) interval = 1;
        if (interval > 20) interval = 20;
        return interval;
    }

    public static FastHopper getInstance() {
        return instance;
    }
}
