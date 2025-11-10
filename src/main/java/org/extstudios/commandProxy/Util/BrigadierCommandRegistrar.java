package org.extstudios.commandProxy.Util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;
import org.extstudios.commandProxy.CommandProxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

public class BrigadierCommandRegistrar {

    private final CommandProxy plugin;
    private final Logger logger;

    public BrigadierCommandRegistrar(CommandProxy plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void registerUppercaseVariants(CommandMap commandMap) {
        try {
            logger.info("Registering uppercase command variants in Brigadier...");

            Map<String, Command> knownCommands = getKnownCommands(commandMap);

            if (knownCommands == null || knownCommands.isEmpty()) {
                logger.severe("Could not get commands via reflection, trying fallback method...");
                knownCommands = getAllCommandsFallback(commandMap);
            }

            if (knownCommands.isEmpty()) {
                logger.severe("failed to get any commands!");
                return;
            }

            logger.info("Found " + knownCommands.size() + " commands to process");

            CommandDispatcher<CommandSourceStack> dispatcher = getBrigadierDispatcher();

            if (dispatcher == null) {
                logger.severe("Failed to get Brigadier dispatcher!");
                return;
            }

            Map<String, Command> commandsSnapshot = new HashMap<>(knownCommands);

            int registeredCount = 0;
            int skippedCount = 0;

            for (Map.Entry<String, Command> entry : commandsSnapshot.entrySet()) {
                String commandName = entry.getKey().toLowerCase(Locale.ROOT);

                if (!commandName.equals(entry.getKey())) continue;

                CommandNode<CommandSourceStack> lowercaseNode = dispatcher.getRoot().getChild(commandName);

                if (lowercaseNode != null) {
                    String uppercaseCommand = commandName.toUpperCase(Locale.ROOT);

                    if (registerVariant(dispatcher, uppercaseCommand, lowercaseNode)) registeredCount++;

                    if (isCommonCommand(commandName)) {
                        List<String> variants = generateCommonVariants(commandName);
                        for (String variant : variants) {
                            if (registerVariant(dispatcher, variant, lowercaseNode)) registeredCount++;
                        }
                    }
                } else {
                    skippedCount++;
                    logger.fine ("Skipping " + commandName + " - not found in Brigadier.");
                }
            }

            logger.info("Successfully registered " + registeredCount + " uppercase command variants!");
            if (skippedCount > 0) {
                logger.severe("Error registering uppercase variants: " + skippedCount + " commands (not in Brigadier tree)");
            }
        } catch (Exception e) {
            logger.severe("Error registering uppercase variants: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean registerVariant(CommandDispatcher<CommandSourceStack> dispatcher,
                                    String variantName,
                                    CommandNode<CommandSourceStack> targetNode) {
        try {
            if (dispatcher.getRoot().getChild(variantName) != null) return false;

            LiteralCommandNode<CommandSourceStack> variantNode =
                    LiteralArgumentBuilder.<CommandSourceStack>literal(variantName)
                            .redirect(targetNode)
                            .build();

            dispatcher.getRoot().addChild(variantNode);

            logger.fine("Registered variant: " + variantName + " -> " + targetNode.getName());
            return true;
        } catch (Exception e) {
            logger.warning("Failed to register variant " + variantName + ": " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands(CommandMap commandMap) {
        try {
            Field knownCommandsField = findField(commandMap.getClass(), "knownCommands");
            if (knownCommandsField == null) {
                logger.warning("Could not find knownCommands field in CommandMap hierarchy");
                return null;
            }

            knownCommandsField.setAccessible(true);
            Map<String, Command> commands = (Map<String, Command>) knownCommandsField.get(commandMap);
            if (commands != null) {
                logger.info("Successfully accessed knownCommands via reflection (" + commands.size() + " commands)");
            }
            return commands;
        } catch (Exception e) {
            logger.severe("Failed to access knownCommands: " + e.getMessage());
            return null;
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;

        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                logger.fine("Found field '" + fieldName + "' in class: " + current.getName());
                return field;
            } catch (NoSuchFieldException e) {
                logger.finest("Field '" + fieldName + "' not in " + current.getName() + "trying superclass");
                current = current.getSuperclass();
            }
        }
        logger.warning("Field '" + fieldName + "' not found in any class in hierarchy");
        return null;
    }

    private Map<String, Command> getAllCommandsFallback(CommandMap commandMap) {
        Map<String, Command> commands = new HashMap<>();

        logger.info("Using fallback method to collect commands...");

        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            Map<String, Map<String, Object>> pluginCommands = p.getDescription().getCommands();

            if (pluginCommands != null) {
                for (String cmdName : pluginCommands.keySet()) {
                    String lowerName = cmdName.toLowerCase(Locale.ROOT);
                    Command cmd = commandMap.getCommand(lowerName);

                    if (cmd != null) {
                        commands.put(lowerName, cmd);

                        for (String alias : cmd.getAliases()) {
                            commands.put(alias.toLowerCase(Locale.ROOT), cmd);
                        }
                    }
                }
            }
        }

        String[] vanillaCommands = {
                "help", "plugins", "pl", "version", "ver", "reload", "rl",
                "timings", "tps", "stop", "save-all", "save-on", "save-off",
                "whitelist", "ban", "ban-ip", "pardon", "pardon-ip", "kick",
                "op", "deop", "tp", "give", "clear", "difficulty", "effect",
                "gamemode", "gamerule", "give", "kill", "list", "me", "msg",
                "say", "seed", "setworldspawn", "spawnpoint", "spreadplayers",
                "summon", "tell", "teleport", "time", "weather", "xp"
        };

        for (String cmdName : vanillaCommands) {
            String lowerName = cmdName.toLowerCase(Locale.ROOT);
            Command cmd = commandMap.getCommand(lowerName);
            if (cmd != null && !commands.containsKey(lowerName)) {
                commands.put(lowerName, cmd);
            }
        }

        logger.info("Fallback method found " + commands.size() + " commands");

        return commands;
    }

    @SuppressWarnings("unchecked")
    private CommandDispatcher<CommandSourceStack> getBrigadierDispatcher() {
        try {
            Method getServerMethod = Bukkit.getServer().getClass().getMethod("getServer");
            Object minecraftServer = getServerMethod.invoke(Bukkit.getServer());

            Method getCommandDispatcherMethod = null;

            try {
                getCommandDispatcherMethod = minecraftServer.getClass().getMethod("getCommands");
            } catch (NoSuchMethodException e) {
                try {
                    getCommandDispatcherMethod = minecraftServer.getClass().getMethod("vanillaCommandDispatcher");
                } catch (NoSuchMethodException ex) {
                    logger.severe("Could not find command dispatcher method");
                    return null;
                }
            }

            Object commands = getCommandDispatcherMethod.invoke(minecraftServer);

            Method getDispatcherMethod = commands.getClass().getMethod("getDispatcher");
            Object dispatcher = getDispatcherMethod.invoke(commands);

            return (CommandDispatcher<CommandSourceStack>) dispatcher;
        } catch (Exception e) {
            logger.severe("Failed to get Brigadier dispatcher :" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean isCommonCommand(String command) {
        List<String> commonCommands = List.of(
                "home", "warp", "spawn", "tpa", "kill", "gamemode",
                "give", "tell", "msg", "help", "list", "back", "fly"
        );
        return commonCommands.contains(command);
    }

    private List<String> generateCommonVariants(String command) {
        List<String> variants = new ArrayList<>();

        variants.add(command.toUpperCase(Locale.ROOT));

        if (command.length() > 1) {
            variants.add(command.substring(0, 1).toUpperCase(Locale.ROOT) +
                    command.substring(1).toUpperCase(Locale.ROOT));
        }

        return variants;
    }
}
