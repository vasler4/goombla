package me.vasler.command.impl.token;

import com.eclipsesource.json.Json;
import java.net.URLConnection;
import java.util.Base64;

public class Headers {
    public static void setHeaders(URLConnection connection, String token) {
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Alt-Used", "discord.com");
        connection.setRequestProperty("Accept-Language", "en-US;q=0.8");
        connection.setRequestProperty("Authorization", token);
        connection.setRequestProperty("Referer", "https://discord.com/channels/@me");
        connection.setRequestProperty("Sec-Ch-Ua", "\"Not?A_Brand\";v=\"8\", \"Chromium\";v=\"108\"");
        connection.setRequestProperty("Sec-Ch-Ua-Mobile", "?0");
        connection.setRequestProperty("Sec-Ch-Ua-Platform", "\"Windows\"");
        connection.setRequestProperty("Sec-Fetch-Dest", "empty");
        connection.setRequestProperty("Sec-Fetch-Mode", "cors");
        connection.setRequestProperty("Sec-Fetch-Site", "same-origin");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/113.0");
        connection.setRequestProperty("X-Debug-Options", "bugReporterEnabled");
        connection.setRequestProperty("X-Discord-Locale", "en-US");
        connection.setRequestProperty("X-Discord-Timezone", "America/Los_Angeles");
        connection.setRequestProperty("X-Super-Properties", Base64.getEncoder().encodeToString(Json.object().add("os", "Windows").add("browser", "Firefox").add("device", "").add("system_locale", "en-US").add("browser_user_agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/113.0").add("browser_version", "113.0").add("os_version", "10").add("referrer", "").add("referring_domain", "").add("referrer_current", "").add("referring_domain_current", "").add("release_channel", "stable").add("client_build_number", 201211).add("client_event_source", Json.NULL).add("design_id", 0).toString().getBytes()));
    }
}

