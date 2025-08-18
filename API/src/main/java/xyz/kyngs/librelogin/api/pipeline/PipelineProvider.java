/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.api.pipeline;

public interface PipelineProvider<P, S> {
    public void registerPipeline(int priority, Pipeline<P, S> pipeline);
    public void unregisterPipeline(int priority);

    public void beginTracking(P player);
    public void cancelTracking(P player);

    public Pipeline<P, S> getPipeline(P player);
    public void next(P player);
    public void finish(P player);
}
