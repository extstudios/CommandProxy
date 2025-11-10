package org.extstudios.commandProxy.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.extstudios.commandProxy.CommandProxy;

import java.util.Map;

public class CommandAliasListener implements Listener {

    private final CommandProxy plugin;

    public CommandAliasListener(CommandProxy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommandAlias(PlayerCommandPreprocessEvent event) {
        if (plugin.isProcessing()) return;

        String message = event.getMessage();
        String[] parts = message.substring(1).split(" ", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        Map<String, String> aliases = plugin.getCommandAliases();

        if(aliases.containsKey(command)) {
            event.setCancelled(true);

            String targetCommand = aliases.get(command);

            targetCommand = processPlaceholders(targetCommand, args, event.getPlayer());

            plugin.setProcessing(true);
            try {
                event.getPlayer().performCommand(targetCommand);
            } finally {
                plugin.setProcessing(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onServerCommandAlias(ServerCommandEvent event) {
        if (plugin.isProcessing()) return;

        String message = event.getCommand();

        String[] parts = message.split(" ", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        Map<String, String> aliases = plugin.getCommandAliases();
        if (aliases.containsKey(command)) {
            event.setCancelled(true);

            String targetCommand = aliases.get(command);

            targetCommand = processPlaceholders(targetCommand, args, null);

            plugin.setProcessing(true);
            try {
                Bukkit.dispatchCommand(event.getSender(), targetCommand);
            } finally {
                plugin.setProcessing(false);
            }
        }
    }

    private String processPlaceholders(String targetCommand, String args, Player player) {
        if (targetCommand.contains("{args}")) {
            targetCommand = targetCommand.replace("{args}", args);
        }

        if (targetCommand.contains("{player}")) {
            String playerName = player != null ? player.getName() : "Console";
            targetCommand = targetCommand.replace("{player}", playerName);
        }

        return targetCommand;
    }
}
