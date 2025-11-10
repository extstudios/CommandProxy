package org.extstudios.commandProxy.Commands;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.extstudios.commandProxy.CommandProxy;

public class ProxyCommand {

    public static LiteralCommandNode<CommandSourceStack> create(CommandProxy plugin) {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("cmdproxy")

        .executes(context -> {
            CommandSourceStack source = context.getSource();

            source.getSender().sendMessage(
                    Component.text("Command Proxy 1.1", NamedTextColor.GOLD)
            );
            source.getSender().sendMessage(
                    Component.text("Intercepting ", NamedTextColor.GRAY)
                            .append(Component.text(
                                    plugin.getTargetCommands().isEmpty() ? "ALL" : plugin.getTargetCommands() + "",
                                    NamedTextColor.YELLOW
                            ))
                            .append(Component.text(" commands", NamedTextColor.GRAY))
            );
            source.getSender().sendMessage(
                    Component.text("Loaded ", NamedTextColor.GRAY)
                            .append(Component.text(
                                    plugin.getCommandAliases().size() + "",
                                    NamedTextColor.YELLOW
                            ))
                            .append(Component.text(" command aliases", NamedTextColor.GRAY))
            );
            source.getSender().sendMessage(
                    Component.text("Use ", NamedTextColor.GRAY)
                            .append(Component.text("/cmdproxy reload", NamedTextColor.YELLOW))
                            .append(Component.text(" to reload config", NamedTextColor.GRAY))
            );

            return Command.SINGLE_SUCCESS;
        })
                .then(Commands.literal("reload")
                        .requires(source ->
                                source.getSender().hasPermission("commandproxy.reload"))
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            plugin.reloadConfiguration();

                            source.getSender().sendMessage(
                                    Component.text("CommandProxy config reloaded!", NamedTextColor.GREEN)
                            );
                            source.getSender().sendMessage(
                                    Component.text("Intercepting ", NamedTextColor.GREEN)
                                            .append(Component.text(
                                                    plugin.getTargetCommands().isEmpty() ? "ALL" : plugin.getTargetCommands().size() + "",
                                                    NamedTextColor.YELLOW
                                            ))
                                            .append(Component.text(" commands", NamedTextColor.GREEN))
                            );
                            source.getSender().sendMessage(
                                    Component.text("Loaded ", NamedTextColor.GREEN)
                                            .append(Component.text(
                                                    plugin.getCommandAliases().size() + "",
                                                    NamedTextColor.YELLOW
                                            ))
                                            .append(Component.text(" command aliases", NamedTextColor.GREEN))
                            );

                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();
    }

}
