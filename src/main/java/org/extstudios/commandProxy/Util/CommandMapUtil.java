package org.extstudios.commandProxy.Util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.util.Set;

public class CommandMapUtil {

    private static CommandMap commandMap;

    public static CommandMap getCommandMap() {
        if (commandMap != null) return commandMap;

        try {
            Field commandmapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandmapField.setAccessible(true);
            commandMap = (CommandMap) commandmapField.get(Bukkit.getServer());
            return commandMap;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean shouldIntercept(String command, Set<String> targetCommands) {
        if (targetCommands.isEmpty()) return true;

        return targetCommands.contains(command.toLowerCase());
    }
}
