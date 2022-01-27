package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.collections4.ListUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.HierarchicalCache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class HierarchicalCacheMaxRateRegistry implements MaxRateRegistry {

    private static final Logger logger = LoggerFactory.getLogger(HierarchicalCacheMaxRateRegistry.class);

    private static final int LEVEL_SUBSCRIPTION = 0;
    private static final int LEVEL_CONSUMER = 1;
    private static final int LEVEL_CONTENT = 2;

    private final CuratorFramework curator;
    private final ObjectMapper objectMapper;
    private final ZookeeperPaths zookeeperPaths;
    private final MaxRatePathSerializer pathSerializer;
    private final HierarchicalCache cache;
    private final SubscriptionsCache subscriptionsCache;
    private final String cluster;

    private final Map<ConsumerInstance, RateInfo> rateInfos = new ConcurrentHashMap<>();

    public HierarchicalCacheMaxRateRegistry(ConfigFactory configFactory, CuratorFramework curator, ObjectMapper objectMapper, ZookeeperPaths zookeeperPaths,
                                            MaxRatePathSerializer pathSerializer, SubscriptionsCache subscriptionsCache) {
        this.curator = curator;
        this.objectMapper = objectMapper;
        this.zookeeperPaths = zookeeperPaths;
        this.pathSerializer = pathSerializer;
        this.subscriptionsCache = subscriptionsCache;
        this.cluster = configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME);

        ThreadFactory cacheThreadFactory = new ThreadFactoryBuilder().setNameFormat("hierarchical-max-rate-registry-%d").build();
        this.cache = new HierarchicalCache(curator,
                Executors.newSingleThreadExecutor(cacheThreadFactory),
                zookeeperPaths.consumersRateRuntimePath(cluster), 3, emptyList(), false
        );

        handleContentUpdates();
        handleConsumerUpdates();
    }

    @Override
    public void start() {
        long startNanos = System.nanoTime();

        loadExistingEntries();
        try {
            cache.start();
        } catch (Exception e) {
            throw new RuntimeException("Unable to start hierarchical cache of max rate registry", e);
        }

        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

        logger.info("Started in {}ms", elapsedMillis);
    }

    @Override
    public void stop() {
        try {
            cache.stop();
        } catch (Exception e) {
            throw new RuntimeException("Unable to stop hierarchical cache of max rate registry", e);
        }
    }

    @Override
    public Set<ConsumerRateInfo> ensureCorrectAssignments(SubscriptionName subscriptionName, Set<String> currentConsumers) {
        Set<ConsumerRateInfo> rateInfos = new HashSet<>();
        try {
            cleanupRegistry(subscriptionName, new ArrayList<>(currentConsumers));

            for (String consumerId : currentConsumers) {
                ConsumerInstance consumerInstance = new ConsumerInstance(consumerId, subscriptionName);
                RateInfo rateInfo = this.rateInfos.getOrDefault(consumerInstance, RateInfo.empty());
                rateInfos.add(new ConsumerRateInfo(consumerId, rateInfo));
            }
        } catch (Exception e) {
            throw new InternalProcessingException("Trouble ensuring assignments in zookeeper", e);
        }
        return rateInfos;
    }

    @Override
    public void update(SubscriptionName subscriptionName, Map<String, MaxRate> newMaxRates) {

        try {
            for (Map.Entry<String, MaxRate> entry : newMaxRates.entrySet()) {
                String maxRatePath = zookeeperPaths.consumersMaxRatePath(cluster, subscriptionName, entry.getKey());
                writeOrCreate(maxRatePath, objectMapper.writeValueAsBytes(entry.getValue()));
            }
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public Optional<MaxRate> getMaxRate(ConsumerInstance consumer) {
        RateInfo rateInfo = rateInfos.get(consumer);
        return Optional.ofNullable(rateInfo).map(RateInfo::getMaxRate).orElse(Optional.empty());
    }

    @Override
    public RateHistory getRateHistory(ConsumerInstance consumer) {
        return Optional.ofNullable(rateInfos.get(consumer)).map(RateInfo::getRateHistory).orElse(RateHistory.empty());
    }

    @Override
    public void writeRateHistory(ConsumerInstance consumer, RateHistory rateHistory) {
        String path = zookeeperPaths.consumersRateHistoryPath(cluster, consumer.getSubscription(), consumer.getConsumerId());
        try {
            byte[] serialized = objectMapper.writeValueAsBytes(rateHistory);
            writeOrCreate(path, serialized);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private void cleanupRegistry(SubscriptionName subscriptionName, List<String> currentConsumers) throws Exception {
        List<String> previousConsumers = rateInfos.keySet().stream() // FIXME what if the previous consumer is not in rateInfos?
                .filter(c -> c.getSubscription().equals(subscriptionName))
                .map(c -> c.getConsumerId()).collect(Collectors.toList());

        List<String> toRemove = ListUtils.subtract(previousConsumers, currentConsumers);

        if (!toRemove.isEmpty()) {
            logger.info("Removing consumers for max rates for subscription {}: {}", subscriptionName, toRemove);
        }

        toRemove.forEach(removedConsumer -> removeConsumerEntries(subscriptionName, removedConsumer));
    }

    private void removeConsumerEntries(SubscriptionName subscriptionName, String consumerId) {
        try {
            curator.delete().deletingChildrenIfNeeded()
                    .forPath(zookeeperPaths.consumersRatePath(cluster, subscriptionName, consumerId));
        } catch (KeeperException.NoNodeException e) {
            // ignore
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private void writeOrCreate(String path, byte[] serializedData) throws Exception {
        try {
            curator.setData().forPath(path, serializedData);
        } catch (KeeperException.NoNodeException e) {
            try {
                curator.create().creatingParentContainersIfNeeded().forPath(path, serializedData);
            } catch (KeeperException.NodeExistsException ex) {
                // ignore
            }
        }
    }

    private void handleConsumerUpdates() {
        cache.registerCallback(LEVEL_CONSUMER, event -> {
            String path = event.getData().getPath();
            ConsumerInstance consumer = pathSerializer.consumerInstanceFromConsumerPath(path);
            switch (event.getType()) {
                case CHILD_REMOVED:
                    rateInfos.remove(consumer);
                    break;
            }
        });
    }

    private void handleContentUpdates() {
        cache.registerCallback(LEVEL_CONTENT, event -> {
            String path = event.getData().getPath();
            byte[] bytes = event.getData().getData();

            ConsumerInstance consumer = pathSerializer.consumerInstanceFromContentPath(path);
            String content = pathSerializer.content(path);

            switch (event.getType()) {
                case CHILD_ADDED:
                case CHILD_UPDATED:
                    switch (content) {
                        case ZookeeperPaths.MAX_RATE_PATH:
                            updateMaxRateInCache(consumer, bytes);
                            break;
                        case ZookeeperPaths.MAX_RATE_HISTORY_PATH:
                            updateRateHistoryInCache(consumer, bytes);
                            break;
                    }
                    break;
            }
        });
    }

    private void updateMaxRateInCache(ConsumerInstance consumer, byte[] bytes) {
        try {
            MaxRate maxRate = objectMapper.readValue(bytes, MaxRate.class);
            rateInfos.compute(consumer, (key, oldValue) -> oldValue == null ?
                    RateInfo.withNoHistory(maxRate) : oldValue.copyWithNewMaxRate(maxRate));
        } catch (Exception e) {
            logger.warn("Problem updating max rate for consumer {}", consumer, e);
        }
    }

    private void updateRateHistoryInCache(ConsumerInstance consumer, byte[] bytes) {
        try {
            RateHistory rateHistory = objectMapper.readValue(bytes, RateHistory.class);
            rateInfos.compute(consumer, (key, oldValue) -> oldValue == null ?
                    RateInfo.withNoMaxRate(rateHistory) : oldValue.copyWithNewRateHistory(rateHistory));
        } catch (Exception e) {
            logger.warn("Problem updating rate history for consumer {}", consumer, e);
        }
    }

    private void loadExistingEntries() {
        List<SubscriptionName> subscriptions = subscriptionsCache.listActiveSubscriptionNames();
        int loadedMaxRates = 0;
        for (SubscriptionName subscriptionName : subscriptions) {
            try {
                List<String> assignedConsumers = consumersInRegistry(subscriptionName);
                if (setInitialMaxRates(subscriptionName, assignedConsumers)) {
                    loadedMaxRates++;
                }
            } catch (Exception e) {
                logger.warn("Exception occurred when initializing cache for subscription {}", subscriptionName, e);
            }
        }
        logger.info("Loaded max-rates of {} out of {} subscriptions", loadedMaxRates, subscriptions.size());
    }

    private List<String> consumersInRegistry(SubscriptionName subscriptionName) throws Exception {
        String subscriptionConsumersPath = zookeeperPaths.consumersRateSubscriptionPath(cluster, subscriptionName);
        return curator.getChildren().forPath(subscriptionConsumersPath);
    }

    private boolean setInitialMaxRates(SubscriptionName subscriptionName, List<String> consumerIds) {
        // It is possible that some stale consumer entries exist. They will be logged.
        // We consider the operation successful when at least one consumer's max rate is read.
        boolean atLeastOneConsumerInitialized = false;
        for (String consumerId : consumerIds) {
            try {
                ConsumerInstance consumer = new ConsumerInstance(consumerId, subscriptionName);
                byte[] rawMaxRate = curator.getData().forPath(
                        zookeeperPaths.consumersMaxRatePath(cluster, subscriptionName, consumerId));
                MaxRate maxRate = objectMapper.readValue(rawMaxRate, MaxRate.class);
                rateInfos.put(consumer, RateInfo.withNoHistory(maxRate));
                atLeastOneConsumerInitialized = true;
            } catch (Exception e) {
                logger.warn(
                        "Exception occurred when initializing cache for subscription {} and consumer {}",
                        subscriptionName, consumerId, e
                );
            }
        }
        return atLeastOneConsumerInitialized;
    }
}
