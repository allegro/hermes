package pl.allegro.tech.hermes.consumers.consumer.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OAuthProvidersNotifyingCacheFactory implements Factory<OAuthProvidersNotifyingCache> {

    private final CuratorFramework curator;

    private final ZookeeperPaths paths;
    private final ObjectMapper objectMapper;

    @Inject
    public OAuthProvidersNotifyingCacheFactory(@Named(CuratorType.HERMES) CuratorFramework curator, ZookeeperPaths paths,
                                               ObjectMapper objectMapper) {
        this.curator = curator;
        this.paths = paths;
        this.objectMapper = objectMapper;
    }

    @Override
    public OAuthProvidersNotifyingCache provide() {
        String path = paths.oAuthProvidersPath();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        OAuthProvidersNotifyingCache cache = new OAuthProvidersNotifyingCache(curator, path, executorService, objectMapper);
        try {
            cache.start();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to start Zookeeper cache for path " + path, e);
        }
        return cache;
    }

    @Override
    public void dispose(OAuthProvidersNotifyingCache instance) {
    }
}
