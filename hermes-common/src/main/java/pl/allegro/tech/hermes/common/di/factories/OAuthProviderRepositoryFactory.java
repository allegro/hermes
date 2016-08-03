package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperOAuthProviderRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import javax.inject.Named;

public class OAuthProviderRepositoryFactory implements Factory<OAuthProviderRepository> {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final ObjectMapper mapper;

    @Inject
    public OAuthProviderRepositoryFactory(@Named(CuratorType.HERMES) CuratorFramework zookeeper, ZookeeperPaths paths,
                                          ObjectMapper mapper) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
    }

    @Override
    public OAuthProviderRepository provide() {
        return new ZookeeperOAuthProviderRepository(zookeeper, mapper, paths);
    }

    @Override
    public void dispose(OAuthProviderRepository instance) {
    }
}
