package pl.allegro.tech.hermes.consumers.subscription.cache.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.cache.zookeeper.StartableCache;
import pl.allegro.tech.hermes.common.cache.queue.QueueTask;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionCallback;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

class SubscriptionsNodeCache extends StartableCache<SubscriptionCallback> implements PathChildrenCacheListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionsNodeCache.class);

    private final ObjectMapper objectMapper;

    private final ExecutorService processingExecutor;

    public SubscriptionsNodeCache(CuratorFramework client, ObjectMapper objectMapper,
                                  String path, ExecutorService eventExecutor, ExecutorService processingExecutor) {

        super(client, path, eventExecutor);
        this.objectMapper = objectMapper;
        this.processingExecutor = processingExecutor;
        getListenable().addListener(this);
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        if (event.getData() == null || event.getData().getData() == null) {
            LOGGER.warn("Unrecognized event {}", event);
            return;
        }
        String path = event.getData().getPath();
        Subscription subscription = readSubscription(event.getData());
        LOGGER.info("Got subscription change event for path {} type {}", path, event.getType());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Event type: {}, event data {}", event.getType().toString(), new String(event.getData().getData(), Charsets.UTF_8));
        }
        switch (event.getType()) {
            case CHILD_ADDED:
                for (SubscriptionCallback callback : callbacks) {
                    processingExecutor.execute(new QueueTask(() -> callback.onSubscriptionCreated(subscription),
                            subscription, subscription.getState(), event.getType()));
                }
                break;
            case CHILD_REMOVED:
                for (SubscriptionCallback callback : callbacks) {
                    processingExecutor.execute(new QueueTask(() -> callback.onSubscriptionRemoved(subscription),
                            subscription, subscription.getState(), event.getType()));
                }
                break;
            case CHILD_UPDATED:
                for (SubscriptionCallback callback : callbacks) {
                    processingExecutor.execute(new QueueTask(() -> callback.onSubscriptionChanged(subscription),
                            subscription, subscription.getState(), event.getType()));
                }
                break;
            default:
                break;
        }
    }

    public List<SubscriptionName> listActiveSubscriptionNames() {
        return getCurrentData().stream()
                .map(this::tryToReadSubscriptionSilently)
                .filter(Objects::nonNull)
                .filter(Subscription::isActive)
                .map(Subscription::toSubscriptionName)
                .collect(Collectors.toList());
    }

    private Subscription tryToReadSubscriptionSilently(ChildData event) {
        try {
            return readSubscription(event);
        } catch (IOException e) {
            return null;
        }
    }

    private Subscription readSubscription(ChildData event) throws IOException {
        return objectMapper.readValue(event.getData(), Subscription.class);
    }
}
