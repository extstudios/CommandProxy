package org.extstudios.commandProxy.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.extstudios.commandProxy.CommandProxy;
import org.extstudios.commandProxy.Util.CommandMapUtil;

public class CommandInterceptListener implements Listener {

    private final CommandProxy plugin;

    public CommandInterceptListener(CommandProxy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if(plugin.isProcessing()) return;

        String message = event.getMessage();

        String[] parts = message.substring(1).split(" ", 2);
        String command = parts[0];

        if (CommandMapUtil.shouldIntercept(command, plugin.getTargetCommands())) {
            String lowercaseCommand = command.toLowerCase();

            if (!command.equals(lowercaseCommand)) {
                event.setCancelled(true);

                String newCommand = "/" + lowercaseCommand;
                if (parts.length > 1) newCommand += " " + parts[1];

                plugin.setProcessing(true);
                try {
                    event.getPlayer().performCommand(newCommand.substring(1));
                } finally {
                    plugin.setProcessing(false);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (plugin.isProcessing()) return;

        String message = event.getCommand();

        String[] parts = message.split(" ", 2);
        String command = parts[0];

        if (CommandMapUtil.shouldIntercept(command, plugin.getTargetCommands())) {
            String lowercaseCommand = command.toLowerCase();

            if (!command.equals(lowercaseCommand)) {
                event.setCancelled(true);

                String newCommand = lowercaseCommand;
                if (parts.length > 1) {
                    newCommand += " " + parts[1];
                }

                plugin.setProcessing(true);
                try {
                    Bukkit.dispatchCommand(event.getSender(), newCommand);
                } finally {
                    plugin.setProcessing(false);
                }
            }
        }
    }
}
