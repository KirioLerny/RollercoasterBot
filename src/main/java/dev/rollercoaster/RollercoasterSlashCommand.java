package dev.rollercoaster;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RollercoasterSlashCommand extends ListenerAdapter {

    private static final int CHANNEL_COUNT = 5;
    private static final long MOVE_DELAY_MS = 750;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!(event.getName().equals("rollercoaster") || event.getName().equals("rc"))) return;

        Member target = event.getOption("ziel").getAsMember();
        if (target == null) {
            event.reply("Der User konnte nicht gefunden werden.").setEphemeral(true).queue();
            return;
        }
        if (target.getVoiceState() == null || !target.getVoiceState().inAudioChannel()) {
            event.reply(target.getEffectiveName() + " befindet sich in keinem Voice-Channel!").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(true).queue(hook -> {
            var guild = event.getGuild();
            assert guild != null;
            VoiceChannel original = target.getVoiceState().getChannel().asVoiceChannel();

            String catName = "Rollercoaster-" + target.getEffectiveName();
            guild.createCategory(catName).queue(category -> createVoiceChannels(category, target, original, hook));
        });
    }

    private void createVoiceChannels(Category category, Member target, VoiceChannel original, InteractionHook hook) {
        List<VoiceChannel> channels = new ArrayList<>();

        for (int i = 1; i <= CHANNEL_COUNT; i++) {
            category.createVoiceChannel("Rollercoaster-" + i).queue(vc -> {
                channels.add(vc);
                if (channels.size() == CHANNEL_COUNT) {
                    startRideSequence(channels, target, original, hook, category);
                }
            });
        }
    }

    private void startRideSequence(List<VoiceChannel> channels, Member target, VoiceChannel original, InteractionHook hook, Category category) {
        List<VoiceChannel> path = new ArrayList<>(channels);
        for (int i = channels.size() - 2; i >= 0; i--) path.add(channels.get(i));

        for (int i = 0; i < path.size(); i++) {
            VoiceChannel vc = path.get(i);
            scheduler.schedule(() -> target.getGuild().moveVoiceMember(target, vc).queue(), MOVE_DELAY_MS * i, TimeUnit.MILLISECONDS);
        }

        long backAt = MOVE_DELAY_MS * path.size();
        scheduler.schedule(() -> target.getGuild().moveVoiceMember(target, original).queue(), backAt, TimeUnit.MILLISECONDS);

        scheduler.schedule(() -> {
            channels.forEach(c -> c.delete().queue());
            category.delete().queue();
            hook.sendMessage("Fahrt beendet für " + target.getAsMention()).queue();
        }, backAt + 1000, TimeUnit.MILLISECONDS);
    }

}
