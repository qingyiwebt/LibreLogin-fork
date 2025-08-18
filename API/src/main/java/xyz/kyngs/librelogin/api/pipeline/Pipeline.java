/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.api.pipeline;

import org.jetbrains.annotations.Nullable;
import xyz.kyngs.librelogin.api.LibreLoginPlugin;
import xyz.kyngs.librelogin.api.database.User;


public abstract class Pipeline<P, S> {
    protected LibreLoginPlugin<P, S> context;

    public Pipeline(LibreLoginPlugin<P, S> context) {
        this.context = context;
    }

    public abstract String getPipelineId();

    public boolean hit(P player, @Nullable User user) {
        return false;
    }
    public void execute(P player, @Nullable User user) {
    }

    public void exit(P player, @Nullable User user) {
    }

    public final void next(P player) {
        context.getPipelineProvider().next(player);
    }
}
