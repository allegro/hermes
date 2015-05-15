package pl.allegro.tech.hermes.consumers.subscription.cache.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.cache.zookeeper.StartableCache;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionCallback;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

class SubscriptionsNodeCache extends StartableCache<SubscriptionCallback> implements PathChildrenCacheListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionsNodeCache.class);

    private final ObjectMapper objectMapper;

    public SubscriptionsNodeCache(CuratorFramework client, ObjectMapper objectMapper,
                                  String path, ExecutorService executorService) {

        super(client, path, executorService);
        this.objectMapper = objectMapper;
        getListenable().addListener(this);
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        if (event.getData() == null || event.getData().getData() == null) {
            LOGGER.warn("Unrecognized event {}", event);
            return;
        }
        String path = event.getData().getPath();
        Subscription subscription = readSubscription(event);
        LOGGER.info("Got subscription change event for path {} type {}", path, event.getType());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Event data {}", new String(event.getData().getData(), Charsets.UTF_8));
        }
        switch (event.getType()) {
            case CHILD_ADDED:
                for (SubscriptionCallback callback : callbacks) {
                    callback.onSubscriptionCreated(subscription);
                }
                break;
            case CHILD_REMOVED:
                for (SubscriptionCallback callback : callbacks) {
                    callback.onSubscriptionRemoved(subscription);
                }
                break;
            case CHILD_UPDATED:
                for (SubscriptionCallback callback : callbacks) {
                    callback.onSubscriptionChanged(subscription);
                }
                break;
            default:
                break;
        }
    }

    private Subscription readSubscription(PathChildrenCacheEvent event) throws IOException {
        return objectMapper.readValue(event.getData().getData(), Subscription.class);
    }

}
