package dev.rollercoaster;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String token = dotenv.get("DISCORD_TOKEN");

        JDA jda = JDABuilder.createDefault(token, GatewayIntent.GUILD_VOICE_STATES)
                .disableCache(CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS)
                .addEventListeners(new RollercoasterSlashCommand(), new RollercoasterGuildListener())
                .build();

        jda.awaitReady();

        jda.updateCommands()
                .addCommands(
                        Commands.slash("rollercoaster", "Start a ride.")
                                .addOption(OptionType.USER, "target", "Target user", true),
                        Commands.slash("rc", "Start a ride.")
                                .addOption(OptionType.USER, "target", "Target user", true)
                ).queue();
    }
}
