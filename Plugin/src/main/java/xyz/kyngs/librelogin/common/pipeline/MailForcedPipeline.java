/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.pipeline;

import org.jetbrains.annotations.Nullable;
import xyz.kyngs.librelogin.api.LibreLoginPlugin;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.api.pipeline.Pipeline;

public class MailForcedPipeline<P, S> extends Pipeline<P, S> {
    public MailForcedPipeline(LibreLoginPlugin<P, S> context) {
        super(context);
    }

    @Override
    public String getPipelineId() {
        return "mail-forced";
    }

    @Override
    public boolean hit(P player, @Nullable User user) {
        if (user == null) {
            return false;
        }

        return user.getEmail() == null || user.getEmail().isEmpty();
    }

    @Override
    public void execute(P player, @Nullable User user) {
        var audience = context.getPlatformHandle().getAudienceForPlayer(player);
        audience.sendMessage(context.getMessages().getMessage("info-email-binding"));
    }
}
