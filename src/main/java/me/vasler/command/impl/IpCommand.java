package me.vasler.command.impl;

import me.vasler.command.Command;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class IpCommand
extends Command {
    public IpCommand() {
        super(new String[]{"ip", "network", "ipaddress"});
    }

    @Override
    public void run(String[] args, TextChannel channel) {
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            channel.sendMessage(bufferedReader.readLine()).queue();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

