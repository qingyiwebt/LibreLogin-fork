/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.api;

import xyz.kyngs.librelogin.api.authorization.AuthorizationProvider;
import xyz.kyngs.librelogin.api.configuration.Messages;
import xyz.kyngs.librelogin.api.crypto.CryptoProvider;
import xyz.kyngs.librelogin.api.crypto.HashedPassword;
import xyz.kyngs.librelogin.api.database.*;
import xyz.kyngs.librelogin.api.database.connector.DatabaseConnector;
import xyz.kyngs.librelogin.api.event.EventProvider;
import xyz.kyngs.librelogin.api.event.EventTypes;
import xyz.kyngs.librelogin.api.image.ImageProjector;
import xyz.kyngs.librelogin.api.integration.LimboIntegration;
import xyz.kyngs.librelogin.api.mail.EmailHandler;
import xyz.kyngs.librelogin.api.pipeline.PipelineProvider;
import xyz.kyngs.librelogin.api.premium.PremiumProvider;
import xyz.kyngs.librelogin.api.server.ServerHandler;
import xyz.kyngs.librelogin.api.totp.TOTPProvider;
import xyz.kyngs.librelogin.api.util.SemanticVersion;
import xyz.kyngs.librelogin.api.util.ThrowableFunction;

import java.io.File;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * The main plugin interface.
 *
 * @param <P> The type of the player
 * @param <S> The type of the server
 */
public interface LibreLoginPlugin<P, S> {

    /**
     * Gets the current premium provider.
     *
     * @return The premium provider
     */
    PremiumProvider getPremiumProvider();

    /**
     * Gets the plugin's logger.
     *
     * @return The logger
     */
    Logger getLogger();

    /**
     * Get the pipeline provider
     * @return The provider
     */
    PipelineProvider<P, S> getPipelineProvider();

    /**
     * Gets a plugin resource as an input stream.
     *
     * @param name The name of the resource
     * @return The input stream, or null if the resource does not exist
     */
    InputStream getResourceAsStream(String name);

    /**
     * Gets the plugin's authorization provider.
     *
     * @return The authorization provider
     */
    AuthorizationProvider<P> getAuthorizationProvider();

    /**
     * Gets the plugin's event provider.
     *
     * @return The event provider
     */
    EventProvider<P, S> getEventProvider();

    /**
     * Gets the plugin's message provider.
     *
     * @return The message provider
     */
    Messages getMessages();

    /**
     * Gets the plugin's crypto provider by the algorithm.
     *
     * @param id The algorithm ID
     * @return The crypto provider
     */
    CryptoProvider getCryptoProvider(String id);

    /**
     * Gets the default crypto provider.
     *
     * @return The default crypto provider
     */
    CryptoProvider getDefaultCryptoProvider();

    /**
     * Gets the plugin's database provider.
     *
     * @return The database provider
     */
    ReadWriteDatabaseProvider getDatabaseProvider();

    /**
     * Gets the plugin's TOTP provider.
     *
     * @return The TOTP provider
     */
    TOTPProvider getTOTPProvider();

    /**
     * Gets the plugin's image projector.
     *
     * @return The image projector
     */
    ImageProjector<P> getImageProjector();

    /**
     * Allows you to migrate the database.
     *
     * @param from The database to migrate from
     * @param to   The database to migrate to
     */
    void migrate(ReadDatabaseProvider from, WriteDatabaseProvider to);

    /**
     * Gets the read providers (immutable).
     *
     * @return The immutable map of read providers
     */
    Map<String, ReadDatabaseProviderRegistration<?, ?, ?>> getReadProviders();

    /**
     * Allows you to use your own read providers.
     * <br>
     * <b>If a {@link ReadWriteDatabaseProvider} is passed, it can also be used as the main database provider for the plugin.</b>
     *
     * @param registration The registration of the provider
     */
    void registerReadProvider(ReadDatabaseProviderRegistration<?, ?, ?> registration);

    /**
     * Allows you to use your own crypto algorithms.
     *
     * @param provider The crypto provider to register
     */
    void registerCryptoProvider(CryptoProvider provider);

    /**
     * Registers a new database connector.
     *
     * @param factory The factory used to create the connector. The string parameter is the configuration prefix.
     * @param clazz   The class the connector will be registered for. (e.g. {@link xyz.kyngs.librelogin.api.database.connector.MySQLDatabaseConnector})
     * @param <C>     The type of the connector
     * @param <E>     The type of the exception
     * @param id      The ID of the connector
     */
    <E extends Exception, C extends DatabaseConnector<E, ?>> void registerDatabaseConnector(Class<?> clazz, ThrowableFunction<String, C, E> factory, String id);

    /**
     * Gets the data folder of the plugin.
     *
     * @return The data folder
     */
    File getDataFolder();

    /**
     * Checks whether the data folder exists.
     */
    void checkDataFolder();

    /**
     * Gets the plugin's version.
     *
     * @return The version
     */
    String getVersion();

    /**
     * Gets the plugin's parsed version.
     *
     * @return The parsed version
     */
    SemanticVersion getParsedVersion();

    /**
     * Checks whether a player with this UUID is present on the network.
     *
     * @param uuid The UUID of the player
     * @return Whether the player is present
     */
    boolean isPresent(UUID uuid);

    /**
     * Checks whether multi-proxy support is enabled.
     *
     * @return Whether multi-proxy support is enabled
     */
    boolean multiProxyEnabled();

    /**
     * Checks whether the password is fine to use.
     *
     * @param password The password to check
     * @return Whether the password is fine to use
     */
    boolean validPassword(String password);

    /**
     * Gets a player by their UUID.
     * <b>This cannot be used as a substitute for {@link #isPresent(UUID)} due to the possibility of multiple proxies.</b>
     *
     * @param uuid The UUID of the player
     * @return The player, or null if the player is not present on this proxy
     */
    P getPlayerForUUID(UUID uuid);

    /**
     * Gets the platform handle.
     *
     * @return The platform handle
     */
    PlatformHandle<P, S> getPlatformHandle();

    /**
     * Gets the server handler.
     * <br>
     * <b>This can be used for registering servers</b>
     *
     * @return The server handler
     */
    ServerHandler<P, S> getServerHandler();

    /**
     * Gets the email handler.
     * <br>
     * <b>This can be used for sending emails</b>
     *
     * @return The email handler, or null if email support is disabled
     */
    @Nullable
    EmailHandler getEmailHandler();

    /**
     * Gets the limbo provider integration.
     * <br>
     * <b>This can be used for creating limbo's</b>
     *
     * @return The limbo provider, or null if no integration was found
     */
    @Nullable
    LimboIntegration<S> getLimboIntegration();

    /**
     * Gets the event types.
     *
     * @return The event types
     */
    default EventTypes<P, S> getEventTypes() {
        return getEventProvider().getTypes();
    }

    /**
     * Returns an implementation of {@link User} containing the given parameters.
     *
     * @param uuid               The UUID of the user, not null
     * @param premiumUUID        The UUID of the user's premium account, nullable
     * @param hashedPassword     The hashed password of the user, nullable
     * @param lastNickname       The last nickname of the user, not null
     * @param joinDate           The join date of the user, not null
     * @param lastSeen           The last seen date of the user, not null
     * @param secret             The TOTP secret of the user, nullable
     * @param ip                 The last IP of the user, nullable
     * @param lastAuthentication The last authentication date of the user, nullable
     * @param lastServer         The last server of the user, nullable
     * @param email              The email of the user, nullable
     * @return an implementation of {@link User} containing the given parameters.
     */
    User createUser(
            UUID uuid,
            UUID premiumUUID,
            HashedPassword hashedPassword,
            String lastNickname,
            Timestamp joinDate,
            Timestamp lastSeen,
            String secret,
            String ip,
            Timestamp lastAuthentication,
            String lastServer,
            String email
    );
}
