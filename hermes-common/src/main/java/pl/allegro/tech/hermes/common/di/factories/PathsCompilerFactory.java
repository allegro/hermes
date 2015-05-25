package pl.allegro.tech.hermes.common.di.factories;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.metric.PathsCompiler;
import pl.allegro.tech.hermes.common.util.HostnameResolver;

import javax.inject.Inject;

public class PathsCompilerFactory implements Factory<PathsCompiler> {

    private final HostnameResolver hostnameResolver;

    @Inject
    public PathsCompilerFactory(HostnameResolver hostnameResolver) {
        this.hostnameResolver = hostnameResolver;
    }

    @Override
    public PathsCompiler provide() {
        return new PathsCompiler(hostnameResolver.resolve());
    }

    @Override
    public void dispose(PathsCompiler instance) {

    }
}
