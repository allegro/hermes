package pl.allegro.tech.hermes.consumers.subscription.cache.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.cache.zookeeper.ZookeeperCacheBase;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionCallback;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;

import javax.inject.Named;
import java.util.Collection;
import java.util.List;

class ZookeeperSubscriptionsCache extends ZookeeperCacheBase implements SubscriptionsCache {

    private final GroupsNodeCache groupsNodeCache;

    public ZookeeperSubscriptionsCache(
            @Named(CuratorType.HERMES) CuratorFramework curatorClient,
            ConfigFactory configFactory,
            ObjectMapper objectMapper) {

        super(configFactory, curatorClient);

        groupsNodeCache = new GroupsNodeCache(curatorClient, objectMapper, paths.groupsPath(), executorService);
    }

    @Override
    public void start(final Collection<? extends SubscriptionCallback> callbacks) {
        Preconditions.checkNotNull(callbacks);
        checkBasePath(() -> groupsNodeCache.start(callbacks));
    }

    @Override
    public void stop() {
        try {
            groupsNodeCache.stop();
            executorService.shutdown();
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    @Override
    public List<SubscriptionName> listActiveSubscriptionNames() {
        return groupsNodeCache.listActiveSubscriptionNames();
    }

}
