package pl.allegro.tech.hermes.common.di.factories;

import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.common.util.InstanceIdResolver;

public class PathsCompilerFactory {

    private final InstanceIdResolver instanceIdResolver;

    public PathsCompilerFactory(InstanceIdResolver instanceIdResolver) {
        this.instanceIdResolver = instanceIdResolver;
    }

    public PathsCompiler provide() {
        return new PathsCompiler(instanceIdResolver.resolve());
    }
}
