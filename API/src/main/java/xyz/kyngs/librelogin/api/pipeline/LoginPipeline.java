package xyz.kyngs.librelogin.api.pipeline;

import org.jetbrains.annotations.Nullable;
import xyz.kyngs.librelogin.api.LibreLoginPlugin;
import xyz.kyngs.librelogin.api.database.User;


public abstract class LoginPipeline<P, S> {
    protected LibreLoginPlugin<P, S> context;

    public LoginPipeline(LibreLoginPlugin<P, S> context) {
        this.context = context;
    }

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
