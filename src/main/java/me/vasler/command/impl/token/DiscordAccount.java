package me.vasler.command.impl.token;

import com.eclipsesource.json.JsonObject;

public class DiscordAccount {
    private final String token;
    private final String id;
    private final String username;
    private final String discriminator;
    private final String email;
    private final String phone;
    private final String avatar;

    public DiscordAccount(String token, String id, String username, String discriminator, String email, String phone, String avatar) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.discriminator = discriminator;
        this.email = email;
        this.phone = phone;
        this.avatar = avatar;
    }

    public static DiscordAccount parse(String token, JsonObject user) {
        return new DiscordAccount(token, user.get("id").asString(), user.get("username").asString(), user.get("discriminator").asString(), user.get("email").asString(), user.get("phone").asString(), user.get("avatar").asString());
    }

    public final String getToken() {
        return this.token;
    }

    public final String getId() {
        return this.id;
    }

    public final String getUsername() {
        return this.username;
    }

    public final String getDiscriminator() {
        return this.discriminator;
    }

    public final String getEmail() {
        return this.email;
    }

    public final String getPhone() {
        return this.phone;
    }

    public final String getAvatar() {
        return this.avatar;
    }
}

