package xyz.kyngs.librelogin.api.pipeline;

public interface PipelineProvider<P, S> {
    public void registerPipeline(int priority, LoginPipeline<P, S> pipeline);
    public void unregisterPipeline(int priority);
    public void beginTracking(P player);
    public void cancelTracking(P player);
    public void next(P player);
    public void finish(P player);
}
