package dev.rollercoaster;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RollercoasterGuildListener extends ListenerAdapter {

    private static final String ADMIN_ROLE_NAME = "Rollercoaster Admin";

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        createAdminRoleIfMissing(event.getGuild());
    }

    private void createAdminRoleIfMissing(Guild guild) {

        boolean exists = guild.getRolesByName(ADMIN_ROLE_NAME, true).stream().findFirst().isPresent();
        if (exists) return;

        guild.createRole()
                .setName(ADMIN_ROLE_NAME)
                .setPermissions(Permission.VIEW_CHANNEL, Permission.VOICE_MOVE_OTHERS)
                .queue();
    }
}
