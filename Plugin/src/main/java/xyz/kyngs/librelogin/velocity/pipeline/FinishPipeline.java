package xyz.kyngs.librelogin.velocity.pipeline;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import xyz.kyngs.librelogin.api.LibreLoginPlugin;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.api.event.exception.EventCancelledException;
import xyz.kyngs.librelogin.api.pipeline.LoginPipeline;

public class FinishPipeline extends LoginPipeline<Player, RegisteredServer> {
    public FinishPipeline(LibreLoginPlugin<Player, RegisteredServer> context) {
        super(context);
    }

    @Override
    public boolean hit(Player player, @Nullable User user) {
        return true;
    }

    @Override
    public void execute(Player player, @Nullable User user) {
        try {
            var lobby = context.getServerHandler().chooseLobbyServer(user, player, true, false);
            if (lobby == null) {
                player.disconnect(context.getMessages().getMessage("kick-no-lobby"));
                return;
            }
            player.createConnectionRequest(lobby).connect().whenComplete((result, throwable) -> {
                if (player.getCurrentServer().isEmpty()) return;
                if (player.getCurrentServer().get().getServerInfo().getName().equals(result.getAttemptedConnection().getServerInfo().getName()))
                    return;
                if (throwable != null || !result.isSuccessful()) player.disconnect(Component.text("Unable to connect"));
            });
        } catch (EventCancelledException ignored) {
        }
    }
}
