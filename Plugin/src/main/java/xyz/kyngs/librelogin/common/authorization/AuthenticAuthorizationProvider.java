/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.authorization;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import xyz.kyngs.librelogin.api.authorization.AuthorizationProvider;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.api.event.events.AuthenticatedEvent;
import xyz.kyngs.librelogin.api.totp.TOTPData;
import xyz.kyngs.librelogin.common.AuthenticHandler;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.config.ConfigurationKeys;
import xyz.kyngs.librelogin.common.event.events.AuthenticAuthenticatedEvent;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AuthenticAuthorizationProvider<P, S> extends AuthenticHandler<P, S> implements AuthorizationProvider<P> {

    private final Map<P, Boolean> unauthorized;
    private final Map<P, String> awaiting2FA;
    private final Cache<UUID, EmailVerifyData> emailConfirmCache;
    private final Cache<UUID, String> passwordResetCache;

    public AuthenticAuthorizationProvider(AuthenticLibreLogin<P, S> plugin) {
        super(plugin);
        unauthorized = new ConcurrentHashMap<>();
        awaiting2FA = new ConcurrentHashMap<>();

        emailConfirmCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();

        passwordResetCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }

    public Cache<UUID, EmailVerifyData> getEmailConfirmCache() {
        return emailConfirmCache;
    }

    public Cache<UUID, String> getPasswordResetCache() {
        return passwordResetCache;
    }

    @Override
    public boolean isAuthorized(P player) {
        return !unauthorized.containsKey(player);
    }

    @Override
    public boolean isAwaiting2FA(P player) {
        return awaiting2FA.containsKey(player);
    }

    @Override
    public void authorize(User user, P player, AuthenticatedEvent.AuthenticationReason reason) {
        if (isAuthorized(player)) {
            throw new IllegalStateException("Player is already authorized");
        }
        stopTracking(player);

        user.setLastAuthentication(Timestamp.valueOf(LocalDateTime.now()));
        user.setIp(platformHandle.getIP(player));
        plugin.getDatabaseProvider().updateUser(user);

        var audience = platformHandle.getAudienceForPlayer(player);

        audience.clearTitle();
        audience.sendActionBar(Component.empty());
        plugin.getEventProvider().fire(plugin.getEventTypes().authenticated, new AuthenticAuthenticatedEvent<>(user, player, plugin, reason));
        plugin.getPipelineProvider().next(player);
    }

    @Override
    public boolean confirmTwoFactorAuth(P player, Integer code, User user) {
        var secret = awaiting2FA.get(player);
        if (plugin.getTOTPProvider().verify(code, secret)) {
            user.setSecret(secret);
            plugin.getDatabaseProvider().updateUser(user);
            return true;
        }
        return false;
    }

    public void startTracking(User user, P player) {
        unauthorized.put(player, user.isRegistered());
    }

    public void stopTracking(P player) {
        awaiting2FA.remove(player);
        emailConfirmCache.invalidate(platformHandle.getUUIDForPlayer(player));
        passwordResetCache.invalidate(platformHandle.getUUIDForPlayer(player));
    }

    public record EmailVerifyData(String email, String token, UUID uuid) {
    }

    public void beginTwoFactorAuth(User user, P player, TOTPData data) {
        awaiting2FA.put(player, data.secret());

        var limbo = plugin.getServerHandler().chooseLimboServer(user, player);

        if (limbo == null) {
            platformHandle.kick(player, plugin.getMessages().getMessage("kick-no-limbo"));
            return;
        }

        platformHandle.movePlayer(player, limbo).whenComplete((t, e) -> {
            if (t != null || e != null) awaiting2FA.remove(player);
        });
    }

    /**
     * Note that the map is not a standalone copy and can be modified!
     */
    public Map<P, Boolean> getUnauthorized() {
        return unauthorized;
    }
}
