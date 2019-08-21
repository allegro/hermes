package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.google.common.base.Preconditions;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_NODE_ID;

class FlatBinaryMaxRateRegistry implements MaxRateRegistry, NodeCacheListener {

    private static final Logger logger = getLogger(FlatBinaryMaxRateRegistry.class);

    private final ZookeeperOperations zookeeper;

    private final Map<String, ConsumerMaxRates> consumersMaxRates = new HashMap<>();
    private final Map<String, ConsumerRateHistory> consumersRateHistories = new HashMap<>();

    private final ConsumerRateHistory currentConsumerRateHistories;
    private final ConsumerMaxRates currentConsumerMaxRates;

    private final String consumerId;
    private final AssignedConsumersSupplier assignedConsumersSupplier;
    private final AssignedSubscriptionsSupplier assignedSubscriptionsSupplier;

    private final ConsumerRateHistoriesEncoder consumerRateHistoriesEncoder;
    private final ConsumerRateHistoriesDecoder consumerRateHistoriesDecoder;
    private final ConsumerMaxRatesDecoder consumerMaxRatesDecoder;
    private final ConsumerMaxRatesEncoder consumerMaxRatesEncoder;

    private final NodeCache maxRateNodeCache;

    private final FlatBinaryMaxRateRegistryPaths registryPaths;

    FlatBinaryMaxRateRegistry(ConfigFactory configFactory,
                                     AssignedConsumersSupplier assignedConsumersSupplier,
                                     AssignedSubscriptionsSupplier assignedSubscriptionsSupplier,
                                     CuratorFramework curator,
                                     ZookeeperPaths zookeeperPaths,
                                     SubscriptionIds subscriptionIds) {

        this.consumerId = configFactory.getStringProperty(CONSUMER_WORKLOAD_NODE_ID);
        this.assignedConsumersSupplier = assignedConsumersSupplier;
        this.assignedSubscriptionsSupplier = assignedSubscriptionsSupplier;
        final String clusterName = configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME);

        this.currentConsumerRateHistories = new ConsumerRateHistory();
        this.currentConsumerMaxRates = new ConsumerMaxRates();

        this.registryPaths = new FlatBinaryMaxRateRegistryPaths(zookeeperPaths, consumerId, clusterName);
        this.zookeeper = new ZookeeperOperations(curator);

        int historiesEncoderBufferSize = configFactory.getIntProperty(Configs.CONSUMER_MAXRATE_REGISTRY_BINARY_ENCODER_HISTORY_BUFFER_SIZE_BYTES);
        this.consumerRateHistoriesEncoder = new ConsumerRateHistoriesEncoder(subscriptionIds, historiesEncoderBufferSize);
        this.consumerRateHistoriesDecoder = new ConsumerRateHistoriesDecoder(subscriptionIds);

        int maxRateEncoderBufferSize = configFactory.getIntProperty(Configs.CONSUMER_MAXRATE_REGISTRY_BINARY_ENCODER_MAX_RATE_BUFFER_SIZE_BYTES);
        this.consumerMaxRatesEncoder = new ConsumerMaxRatesEncoder(subscriptionIds, maxRateEncoderBufferSize);
        this.consumerMaxRatesDecoder = new ConsumerMaxRatesDecoder(subscriptionIds);

        this.maxRateNodeCache = new NodeCache(curator, registryPaths.consumerMaxRatePath(consumerId));
        maxRateNodeCache.getListenable().addListener(this);
    }

    @Override
    public void start() {
        try {
            logger.info("Starting flat binary max rate registry at {}, watching current consumer path at {}",
                    registryPaths.consumersRateCurrentClusterRuntimeBinaryPath(), registryPaths.consumerMaxRatePath(consumerId));
            maxRateNodeCache.start();
        } catch (Exception e) {
            throw new IllegalStateException("Could not start node cache for consumer max rate", e);
        }
        refreshConsumerMaxRates();
    }

    private void refreshConsumerMaxRates() {
        ChildData nodeData = maxRateNodeCache.getCurrentData();
        if (nodeData != null) {
            byte[] data = nodeData.getData();
            ConsumerMaxRates decodedMaxRates = consumerMaxRatesDecoder.decode(data);
            logger.info("Decoded {} bytes of max rates for current node with {} subscription entries", data.length, decodedMaxRates.size());
            currentConsumerMaxRates.setAllMaxRates(decodedMaxRates);
        }
    }

    @Override
    public void stop() {
        try {
            logger.info("Stopping flat binary max rate registry");
            maxRateNodeCache.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not stop node cache for consumer max rate", e);
        }
    }

    @Override
    public void onBeforeMaxRateCalculation() {
        Set<String> assignedConsumers = assignedConsumersSupplier.getAllAssignedConsumers();
        clearCacheFromInactiveConsumers(assignedConsumers);
        refreshRateCachesOfConsumers(assignedConsumers);
    }

    private void clearCacheFromInactiveConsumers(Set<String> assignedConsumers) {
        consumersMaxRates.entrySet().removeIf(entry -> !assignedConsumers.contains(entry.getKey()));
        consumersRateHistories.entrySet().removeIf(entry -> !assignedConsumers.contains(entry.getKey()));
    }

    private void refreshRateCachesOfConsumers(Set<String> assignedConsumers) {
        getMaxRateConsumerNodes().forEach(consumerId -> {
            if (assignedConsumers.contains(consumerId)) {
                refreshConsumerRateHistory(consumerId);
                refreshConsumerMaxRate(consumerId);
            } else {
                removeConsumerRateRootNode(consumerId);
            }
        });
    }

    private List<String> getMaxRateConsumerNodes() {
        String path = registryPaths.consumersRateCurrentClusterRuntimeBinaryPath();
        try {
            if (zookeeper.exists(path)) {
                return zookeeper.getNodeChildren(path);
            }
        } catch (Exception e) {
            logger.warn("Could not get max rate consumer nodes list", e);
        }
        return Collections.emptyList();
    }

    private void refreshConsumerMaxRate(String consumerId) {
        logger.info("Refreshing max rate of {}", consumerId);
        String consumerMaxRatePath = registryPaths.consumerMaxRatePath(consumerId);
        zookeeper.getNodeData(consumerMaxRatePath)
                .map(consumerMaxRatesDecoder::decode)
                .ifPresent(maxRates -> {
                    int decodedSize = maxRates.size();
                    maxRates.cleanup(assignedSubscriptionsSupplier.getAssignedSubscriptions(consumerId));
                    int cleanedSize = maxRates.size();
                    if (decodedSize > cleanedSize) {
                        logger.info("Refreshed max rates of {} with {} subscriptions ({} stale entries omitted)",
                                consumerId, cleanedSize, decodedSize - cleanedSize);
                    } else {
                        logger.info("Refreshed max rates of {} with {} subscriptions", consumerId, cleanedSize);
                    }
                    consumersMaxRates.put(consumerId, maxRates);
                });
    }

    private void refreshConsumerRateHistory(String consumerId) {
        logger.info("Refreshing rate history of {}", consumerId);
        String consumerRateHistoryPath = registryPaths.consumerRateHistoryPath(consumerId);
        zookeeper.getNodeData(consumerRateHistoryPath)
                .map(consumerRateHistoriesDecoder::decode)
                .ifPresent(rateHistories -> {
                    logger.info("Refreshed rate history of {} with {} subscriptions", consumerId, rateHistories.size());
                    consumersRateHistories.put(consumerId, rateHistories);
                });
    }

    private void removeConsumerRateRootNode(String consumerId) {
        logger.info("Deleting max rate node of stale consumer {}", consumerId);
        String path = registryPaths.consumerRateParentRuntimePath(consumerId);
        try {
            zookeeper.deleteNodeRecursively(path);
        } catch (Exception e) {
            logger.warn("Could not delete stale consumer max rate node {}", path, e);
        }
    }

    @Override
    public void onAfterMaxRateCalculation() {
        persistMaxRatesForAllConsumers();
    }

    private void persistMaxRatesForAllConsumers() {
        consumersMaxRates.forEach((consumerId, maxRates) -> {
            byte[] encoded = consumerMaxRatesEncoder.encode(maxRates);
            String consumerMaxRatePath = registryPaths.consumerMaxRatePath(consumerId);
            try {
                zookeeper.writeOrCreatePersistent(consumerMaxRatePath, encoded);
            } catch (Exception e) {
                logger.warn("Could not write max rates for consumer {}", consumerId, e);
            }
        });
    }

    @Override
    public Set<ConsumerRateInfo> ensureCorrectAssignments(SubscriptionName subscriptionName, Set<String> currentConsumers) {
        Set<ConsumerRateInfo> rateInfos = new HashSet<>();
        for (String consumerId : currentConsumers) {
            Optional<MaxRate> maxRate = Optional.ofNullable(consumersMaxRates.get(consumerId))
                    .flatMap(rates -> rates.getMaxRate(subscriptionName));
            RateHistory rateHistory = Optional.ofNullable(consumersRateHistories.get(consumerId))
                    .map(histories -> histories.getRateHistory(subscriptionName))
                    .orElse(RateHistory.empty());
            rateInfos.add(new ConsumerRateInfo(consumerId, new RateInfo(maxRate, rateHistory)));
        }
        return rateInfos;
    }

    @Override
    public void update(SubscriptionName subscriptionName, Map<String, MaxRate> newMaxRates) {
        newMaxRates.forEach((consumerId, maxRate) -> {
            consumersMaxRates.putIfAbsent(consumerId, new ConsumerMaxRates());
            consumersMaxRates.get(consumerId).setMaxRate(subscriptionName, maxRate);
        });
    }

    @Override
    public Optional<MaxRate> getMaxRate(ConsumerInstance consumer) {
        Preconditions.checkState(consumer.getConsumerId().equals(consumerId), "Reading max rate is allowed only for current consumer");
        return currentConsumerMaxRates.getMaxRate(consumer.getSubscription());
    }

    @Override
    public RateHistory getRateHistory(ConsumerInstance consumer) {
        Preconditions.checkState(consumer.getConsumerId().equals(consumerId), "Reading rate history is allowed only for current consumer");
        return currentConsumerRateHistories.getRateHistory(consumer.getSubscription());
    }

    @Override
    public void writeRateHistory(ConsumerInstance consumer, RateHistory rateHistory) {
        Preconditions.checkState(consumer.getConsumerId().equals(consumerId), "Saving rate history is allowed only for current consumer");
        currentConsumerRateHistories.setRateHistory(consumer.getSubscription(), rateHistory);
    }

    @Override
    public void onAfterWriteRateHistories() {
        Set<SubscriptionName> subscriptions = assignedSubscriptionsSupplier.getAssignedSubscriptions(consumerId);
        currentConsumerRateHistories.cleanup(subscriptions);
        byte[] encoded = consumerRateHistoriesEncoder.encode(currentConsumerRateHistories);
        logger.info("Writing rate history of {} subscriptions, saving {} bytes", currentConsumerRateHistories.size(), encoded.length);
        try {
            zookeeper.writeOrCreatePersistent(registryPaths.currentConsumerRateHistoryPath(), encoded);
        } catch (Exception e) {
            logger.error("An error while saving consumers rate histories");
        }
    }

    @Override
    public void nodeChanged() {
        refreshConsumerMaxRates();
    }

    @FunctionalInterface
    interface AssignedSubscriptionsSupplier {
        Set<SubscriptionName> getAssignedSubscriptions(String consumerId);
    }

    @FunctionalInterface
    interface AssignedConsumersSupplier {
        Set<String> getAllAssignedConsumers();
    }
}
