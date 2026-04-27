package de.fasthopper;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;

public class FastHopperCommand implements CommandExecutor, TabCompleter {

    private final FastHopper plugin;

    public FastHopperCommand(FastHopper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fasthopper.admin")) {
            sender.sendMessage(ChatColor.RED + "Du hast keine Berechtigung für diesen Befehl.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "=== FastHopper ===");
            sender.sendMessage(ChatColor.YELLOW + "Aktuelles Interval: " + ChatColor.WHITE
                    + plugin.getHopperInterval() + " Transfer(s) pro Hopper-Tick");
            sender.sendMessage(ChatColor.YELLOW + "Verwendung: /fasthopper set <1-20>");
            sender.sendMessage(ChatColor.YELLOW + "             /fasthopper reload");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Verwendung: /fasthopper set <1-20>");
                    return true;
                }
                try {
                    int value = Integer.parseInt(args[1]);
                    if (value < 1 || value > 20) {
                        sender.sendMessage(ChatColor.RED + "Wert muss zwischen 1 und 20 liegen.");
                        return true;
                    }
                    plugin.getConfig().set("hopper-interval", value);
                    plugin.saveConfig();
                    sender.sendMessage(ChatColor.GREEN + "Hopper-Interval wurde auf "
                            + ChatColor.WHITE + value + ChatColor.GREEN + " gesetzt.");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Bitte eine gültige Zahl eingeben.");
                }
                break;

            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "FastHopper Konfiguration neu geladen. "
                        + "Interval: " + plugin.getHopperInterval());
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unbekannter Befehl. Verwende /fasthopper");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Arrays.asList("1", "2", "4", "8", "10", "16", "20");
        }
        return List.of();
    }
}
