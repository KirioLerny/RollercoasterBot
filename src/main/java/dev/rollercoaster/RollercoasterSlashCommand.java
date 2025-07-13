package dev.rollercoaster;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class RollercoasterSlashCommand extends ListenerAdapter {

    private static final Logger LOGGER = Logger.getLogger(RollercoasterSlashCommand.class.getName());

    private static final int CHANNEL_COUNT = 5;
    private static final long MOVE_DELAY_MS = 750;

    private static final Set<Long> ALLOWED_USER_IDS = Set.of(340774639821127680L, 435438930666717185L);
    private static final String ADMIN_ROLE_NAME = "Rollercoaster Admin";

    private final Set<Long> activeRiders = ConcurrentHashMap.newKeySet();
    private final Map<Long, Category> riderCategories = new ConcurrentHashMap<>();
    private final Map<Long, List<ScheduledFuture<?>>> riderTasks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (!(event.getName().equals("rollercoaster") || event.getName().equals("rc"))) return;

        Member invoker = event.getMember();
        if (invoker == null || !isInvokerAllowed(invoker)) {
            event.reply("You do not have permission to run this command.").setEphemeral(true).queue();
            return;
        }

        var targetOption = event.getOption("target");
        if (targetOption == null) targetOption = event.getOption("ziel");
        if (targetOption == null) {
            event.reply("❗ Please specify a user to put on the rollercoaster.").setEphemeral(true).queue();
            return;
        }
        Member target = targetOption.getAsMember();
        if (target == null) {
            event.reply("Could not find the specified user.").setEphemeral(true).queue();
            return;
        }

        if (target.getVoiceState() == null || !target.getVoiceState().inAudioChannel()) {
            event.reply(target.getEffectiveName() + " is not in a voice channel!").setEphemeral(true).queue();
            return;
        }
        if (!activeRiders.add(target.getIdLong())) {
            event.reply(target.getEffectiveName() + " is already on a ride!").setEphemeral(true).queue();
            return;
        }

        LOGGER.info(() -> String.format("%s (%s) executed /rollercoaster on %s (%s) in guild %s (%s)",
                invoker.getEffectiveName(), invoker.getId(),
                target.getEffectiveName(), target.getId(),
                Objects.requireNonNull(event.getGuild()).getName(), event.getGuild().getId()));

        event.deferReply().setEphemeral(true).queue(hook -> {
            var guild = event.getGuild();
            assert guild != null;
            VoiceChannel original = Objects.requireNonNull(target.getVoiceState().getChannel()).asVoiceChannel();

            String catName = "Rollercoaster - " + target.getEffectiveName();
            guild.createCategory(catName).queue(category -> {
                riderCategories.put(target.getIdLong(), category);
                createVoiceChannels(category, target, original, hook);
            });
        });
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Member member = event.getMember();
        long userId = member.getIdLong();
        if (!activeRiders.contains(userId)) return;

        Category coasterCat = riderCategories.get(userId);

        boolean leftGuildVoice = event.getNewValue() == null;
        boolean leftCategory = event.getNewValue() != null && !Objects.equals(event.getNewValue().getParentCategory(), coasterCat);
        if (leftGuildVoice || leftCategory) {
            LOGGER.info(member.getEffectiveName() + " left the rollercoaster prematurely – aborting ride.");
            abortRide(member);
        }
    }

    private boolean isInvokerAllowed(Member invoker) {
        if (invoker.hasPermission(Permission.ADMINISTRATOR)) return true;
        if (ALLOWED_USER_IDS.contains(invoker.getIdLong())) return true;
        Role adminRole = invoker.getGuild().getRolesByName(ADMIN_ROLE_NAME, true).stream()
                .findFirst().orElse(null);
        return adminRole != null && invoker.getRoles().contains(adminRole);
    }

    private void createVoiceChannels(Category category, Member target, VoiceChannel original, InteractionHook hook) {
        List<VoiceChannel> channels = new ArrayList<>();

        for (int i = 1; i <= CHANNEL_COUNT; i++) {
            category.createVoiceChannel("Rollercoaster - " + i).queue(vc -> {
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

        List<ScheduledFuture<?>> tasks = new ArrayList<>();

        for (int i = 0; i < path.size(); i++) {
            VoiceChannel vc = path.get(i);
            ScheduledFuture<?> task = scheduler.schedule(() -> target.getGuild().moveVoiceMember(target, vc).queue(),
                    MOVE_DELAY_MS * i, TimeUnit.MILLISECONDS);
            tasks.add(task);
        }

        long backAt = MOVE_DELAY_MS * path.size();
        ScheduledFuture<?> backTask = scheduler.schedule(() -> target.getGuild().moveVoiceMember(target, original).queue(), backAt, TimeUnit.MILLISECONDS);
        tasks.add(backTask);

        ScheduledFuture<?> cleanupTask = scheduler.schedule(() -> finishRide(target, hook, channels, category), backAt + 1000, TimeUnit.MILLISECONDS);
        tasks.add(cleanupTask);

        riderTasks.put(target.getIdLong(), tasks);
    }

    private void finishRide(Member target, InteractionHook hook, List<VoiceChannel> channels, Category category) {
        channels.forEach(c -> c.delete().queue());
        category.delete().queue();
        hook.sendMessage("Ride finished for " + target.getAsMention()).queue();
        hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
        cleanupTracking(target.getIdLong());
    }

    private void abortRide(Member rider) {

        long id = rider.getIdLong();
        List<ScheduledFuture<?>> tasks = riderTasks.remove(id);
        if (tasks != null) tasks.forEach(t -> t.cancel(false));

        Category cat = riderCategories.remove(id);
        if (cat != null) {
            cat.getChannels().forEach(ch -> ch.delete().queue());
            cat.delete().queue();
        }
        cleanupTracking(id);
    }

    private void cleanupTracking(long userId) {
        activeRiders.remove(userId);
        riderCategories.remove(userId);
        riderTasks.remove(userId);
    }

    // TODO: Wenn jemand unabhängiges in den rollercoaster geschoben wird soll der rollercoaster mit der ursprünglichen person weiterlaufen und die falsche person soll in den ursprünglichen channel gemoved werden
}
