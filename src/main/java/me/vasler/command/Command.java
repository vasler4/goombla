package me.vasler.command;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.IOException;

public abstract class Command {
    String[] alias;

    public Command(String[] alias) {
        this.alias = alias;
    }

    public abstract void run(String[] var1, TextChannel var2) throws IOException;

    public String[] getAlias() {
        return this.alias;
    }
}

