package xyz.kyngs.librelogin.common.pipeline;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.api.pipeline.LoginPipeline;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.config.ConfigurationKeys;

import java.time.Duration;
import java.util.HashSet;

public class AuthPipeline<P, S> extends LoginPipeline<P, S> {
    private final AuthenticLibreLogin<P, S> context;
    public AuthPipeline(AuthenticLibreLogin<P, S> context) {
        super(context);
        this.context = context;

        var millis = context.getConfiguration().get(ConfigurationKeys.MILLISECONDS_TO_REFRESH_NOTIFICATION);

        if (millis > 0) {
            context.repeat(this::notifyUnauthorized, 0, millis);
        }
        context.repeat(this::broadcastActionBars, 0, 1000);
    }

    @Override
    public boolean hit(P player, @Nullable User user) {
        return true;
    }

    @Override
    public void execute(P player, User user) {
        var platformHandle = context.getPlatformHandle();
        var authorizationProvider = context.getAuthorizationProvider();
        var audience = platformHandle.getAudienceForPlayer(player);

        authorizationProvider.startTracking(user, player);
        context.cancelOnExit(context.delay(() -> {
            if (authorizationProvider.isAuthorized(player)) return;
            sendInfoMessage(user.isRegistered(), audience);
        }, 250), player);

        var limit = context.getConfiguration().get(ConfigurationKeys.SECONDS_TO_AUTHORIZE);

        if (limit > 0) {
            context.cancelOnExit(context.delay(() -> {
                if (authorizationProvider.isAuthorized(player)) return;
                platformHandle.kick(player, context.getMessages().getMessage("kick-time-limit"));
            }, limit * 1000L), player);
        }

        sendInfoMessage(user.isRegistered(), audience);
    }

    @Override
    public void exit(P player, @Nullable User user) {
        var authorizationProvider = context.getAuthorizationProvider();
        authorizationProvider.stopTracking(player);
    }

    public void notifyUnauthorized() {
        var platformHandle = context.getPlatformHandle();
        var authorizationProvider = context.getAuthorizationProvider();
        var wrong = new HashSet<P>();
        var unauthorized = authorizationProvider.getUnauthorized();

        unauthorized.forEach((player, registered) -> {
            var audience = platformHandle.getAudienceForPlayer(player);
            if (audience == null) {
                wrong.add(player);
                return;
            }

            sendInfoMessage(registered, audience);
        });

        wrong.forEach(unauthorized::remove);
    }

    private void sendInfoMessage(boolean registered, Audience audience) {
        audience.sendMessage(context.getMessages().getMessage(registered ? "prompt-login" : "prompt-register"));
        if (!context.getConfiguration().get(ConfigurationKeys.USE_TITLES)) return;
        var toRefresh = context.getConfiguration().get(ConfigurationKeys.MILLISECONDS_TO_REFRESH_NOTIFICATION);
        //noinspection UnstableApiUsage
        audience.showTitle(Title.title(
                context.getMessages().getMessage(registered ? "title-login" : "title-register"),
                context.getMessages().getMessage(registered ? "sub-title-login" : "sub-title-register"),
                Title.Times.of(
                        Duration.ofMillis(0),
                        Duration.ofMillis(toRefresh > 0 ?
                                (long) (toRefresh * 1.1) :
                                10000
                        ),
                        Duration.ofMillis(0)
                )
        ));
    }


    private void broadcastActionBars() {
        var platformHandle = context.getPlatformHandle();
        var authorizationProvider = context.getAuthorizationProvider();
        var wrong = new HashSet<P>();
        var unauthorized = authorizationProvider.getUnauthorized();

        unauthorized.forEach((player, registered) -> {
            var audience = platformHandle.getAudienceForPlayer(player);
            if (audience == null) {
                wrong.add(player);
                return;
            }

            sendActionBar(registered, audience);
        });

        wrong.forEach(unauthorized::remove);
    }

    private void sendActionBar(boolean registered, Audience audience) {
        if (context.getConfiguration().get(ConfigurationKeys.USE_ACTION_BAR)) {
            audience.sendActionBar(context.getMessages().getMessage(registered ? "action-bar-login" : "action-bar-register"));
        }
    }
}
