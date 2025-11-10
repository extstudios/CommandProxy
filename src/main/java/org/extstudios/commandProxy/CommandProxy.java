package org.extstudios.commandProxy;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.extstudios.commandProxy.Commands.ProxyCommand;
import org.extstudios.commandProxy.Listeners.CommandAliasListener;
import org.extstudios.commandProxy.Listeners.CommandInterceptListener;
import org.extstudios.commandProxy.Util.BrigadierCommandRegistrar;
import org.extstudios.commandProxy.Util.CommandMapUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class CommandProxy extends JavaPlugin {

    private static CommandProxy instance;
    private Set<String> targetCommands;
    private Map<String, String> commandAliases;
    private CommandMap commandMap;
    private boolean processing = false;
    private BrigadierCommandRegistrar brigadierRegistrar;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        commandMap = CommandMapUtil.getCommandMap();
        if (commandMap == null) {
            getLogger().severe("Failed to access CommandMap! Plugin may not work correctly.");
        }

        loadTargetCommands();
        registerListeners();
        registerCommands();

        brigadierRegistrar = new BrigadierCommandRegistrar(this);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            brigadierRegistrar.registerUppercaseVariants(commandMap);
        }, 20L);

        getLogger().info("CommandLowercaseProxy has been enabled!");
        getLogger().info("Intercepting " + (targetCommands.isEmpty() ? "ALL" : targetCommands.size()) + " commands");
        getLogger().info("Loaded " + commandAliases.size() + " command aliases");
        getLogger().info("Supports: Bukkit, Spigot, old Paper reflection, and new Paper Brigadier commands!");
        getLogger().info("Uppercase command variants will be registered in Brigadier shortly...");
    }

    @Override
    public void onDisable() {
        getLogger().info("CommandLowercaseProxy has been disabled!");
        instance = null;
    }

    private void loadTargetCommands() {
        targetCommands = new HashSet<>();

        if (getConfig().contains("target-commands")) {
            targetCommands.addAll(getConfig().getStringList("target-commands"));
        }

        Set<String> lowercaseCommands = new HashSet<>();
        for (String cmd : targetCommands) {
            lowercaseCommands.add(cmd.toLowerCase());
        }
        targetCommands = lowercaseCommands;

        loadCommandAliases();
    }

    private void loadCommandAliases() {
        commandAliases = new HashMap<>();

        if (getConfig().contains("command-aliases")) {
            var aliasesSection = getConfig().getConfigurationSection("command-aliases");
            if (aliasesSection != null) {
                for (String alias : aliasesSection.getKeys(false)) {
                    String targetCommand = aliasesSection.getString(alias);
                    if (targetCommand != null && !targetCommand.isEmpty()) {
                        commandAliases.put(alias.toLowerCase(), targetCommand);
                        getLogger().info("Loaded alias: /" + alias + "-> /" + targetCommand);
                    }
                }
            }
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CommandInterceptListener(this), this);
        //getServer().getPluginManager().registerEvents(new TabCompleteListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandAliasListener(this), this);
    }

    private void registerCommands() {
        LifecycleEventManager<@NotNull Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    ProxyCommand.create(this),
                    "Main command for CommandLowercaseProxy",
                    List.of("commandproxy", "cmdlower")
            );
        });
    }

    public void reloadConfiguration() {
        reloadConfig();
        loadTargetCommands();

        if (brigadierRegistrar != null && commandMap != null) {
            Bukkit.getScheduler().runTask(this, () -> {
                brigadierRegistrar.registerUppercaseVariants(commandMap);
            });
        }
    }

    public static CommandProxy getInstance() {
        return instance;
    }

    public Set<String> getTargetCommands() {
        return  targetCommands;
    }

    public Map<String, String> getCommandAliases() {
        return commandAliases;
    }

    public CommandMap getCommandMap() {
        return commandMap;
    }

    public boolean isProcessing() {
        return processing;
    }


    public void setProcessing(boolean processing) {
        this.processing = processing;
    }

}
