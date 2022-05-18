package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperOAuthProviderRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

public class OAuthProviderRepositoryFactory {

    private final CuratorFramework zookeeper;

    private final ZookeeperPaths paths;

    private final ObjectMapper mapper;

    public OAuthProviderRepositoryFactory(CuratorFramework zookeeper, ZookeeperPaths paths,
                                          ObjectMapper mapper) {
        this.zookeeper = zookeeper;
        this.paths = paths;
        this.mapper = mapper;
    }

    public OAuthProviderRepository provide() {
        return new ZookeeperOAuthProviderRepository(zookeeper, mapper, paths);
    }
}
