package dev.rollercoaster;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("DISCORD_TOKEN");

        JDA jda = JDABuilder.createDefault(token, GatewayIntent.GUILD_VOICE_STATES)
                .addEventListeners(new RollercoasterSlashCommand())
                .build();

        jda.awaitReady();

        /*
        // Nur beim ersten Start oder beim Command-Update notwendig
        jda.updateCommands().addCommands(
                Commands.slash("rollercoaster", "Start a rollercoaster ride!")
                        .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.USER,
                                "ziel", "User, der gefahren werden soll", true),
                Commands.slash("rc", "Start a rollercoaster ride!")
                        .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.USER,
                                "ziel", "User, der gefahren werden soll", true)
        ).queue();

         */
    }
}
