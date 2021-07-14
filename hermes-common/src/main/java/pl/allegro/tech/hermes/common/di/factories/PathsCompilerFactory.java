package pl.allegro.tech.hermes.common.di.factories;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.common.util.InstanceIdResolver;

import javax.inject.Inject;

public class PathsCompilerFactory implements Factory<PathsCompiler> {

    private final InstanceIdResolver instanceIdResolver;

    @Inject
    public PathsCompilerFactory(InstanceIdResolver instanceIdResolver) {
        this.instanceIdResolver = instanceIdResolver;
    }

    @Override
    public PathsCompiler provide() {
        return new PathsCompiler(instanceIdResolver.resolve());
    }

    @Override
    public void dispose(PathsCompiler instance) {

    }
}
