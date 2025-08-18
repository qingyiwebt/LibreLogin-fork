/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.pipeline;

import org.jetbrains.annotations.Nullable;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.api.pipeline.Pipeline;
import xyz.kyngs.librelogin.api.pipeline.PipelineProvider;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPipelineProvider<P, S> implements PipelineProvider<P, S> {
    private final Map<P, Integer> pipelineStatus;
    private final NavigableMap<Integer, Pipeline<P, S>> pipelines;
    private final AuthenticLibreLogin<P, S> context;

    public DefaultPipelineProvider(AuthenticLibreLogin<P, S> context) {
        this.context = context;
        this.pipelineStatus = new ConcurrentHashMap<>();
        this.pipelines = new TreeMap<>();
    }

    @Override
    public void registerPipeline(int priority, Pipeline<P, S> pipeline) {
        pipelines.put(priority, pipeline);
    }

    @Override
    public void unregisterPipeline(int priority) {
        pipelines.remove(priority);
    }

    @Override
    public void beginTracking(P player) {
        this.jump(player, getUser(player), pipelines.firstKey());
    }

    @Override
    public void cancelTracking(P player) {
        var currentPipelinePriority = pipelineStatus.remove(player);
        if (currentPipelinePriority != null) {
            var pipeline = pipelines.get(currentPipelinePriority);
            var user = getUser(player);
            pipeline.exit(player, user);
        }
    }

    @Override
    public Pipeline<P, S> getPipeline(P player) {
        var pipelinePriority = pipelineStatus.get(player);
        if (pipelinePriority == null) {
            return null;
        }

        return pipelines.get(pipelinePriority);
    }

    @Nullable
    private User getUser(P player) {
        var uuid = context.getPlatformHandle().getUUIDForPlayer(player);
        return context.getDatabaseProvider().getByUUID(uuid);
    }

    private void jump(P player, @Nullable User user, Integer priority) {
        var previousPipelinePriority = pipelineStatus.put(player, priority);
        if (previousPipelinePriority != null) {
            var previousPipeline = pipelines.get(previousPipelinePriority);
            previousPipeline.exit(player, user);
        }
        var pipeline = pipelines.get(priority);
        pipeline.execute(player, user);
    }

    @Override
    public void next(P player) {
        var currentPipelinePriority = pipelineStatus.get(player);
        if (currentPipelinePriority == null) {
            return;
        }

        var user = getUser(player);
        Integer newPipelinePriority = currentPipelinePriority;
        while (true) {
            newPipelinePriority = pipelines.higherKey(newPipelinePriority);
            if (newPipelinePriority == null) {
                return;
            }

            var pipeline = pipelines.get(newPipelinePriority);
            if (pipeline.hit(player, user)) {
                break;
            }
        }

        this.jump(player, user, newPipelinePriority);
    }

    @Override
    public void finish(P player) {
        var user = this.getUser(player);
        this.jump(player, user, pipelines.lastKey());
    }
}
