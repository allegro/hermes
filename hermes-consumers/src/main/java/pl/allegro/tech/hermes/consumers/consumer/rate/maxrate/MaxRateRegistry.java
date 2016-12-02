package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.ListUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MaxRateRegistry {

    private static final Logger logger = LoggerFactory.getLogger(MaxRateRegistry.class);

    private final CuratorFramework curator;
    private final ObjectMapper objectMapper;
    private final ZookeeperPaths zookeeperPaths;

    @Inject
    public MaxRateRegistry(CuratorFramework curator, ObjectMapper objectMapper, ZookeeperPaths zookeeperPaths) {
        this.curator = curator;
        this.objectMapper = objectMapper;
        this.zookeeperPaths = zookeeperPaths;
    }

    Set<ConsumerRateInfo> ensureCorrectAssignments(SubscriptionName subscriptionName, Set<String> currentConsumers) {
        Set<ConsumerRateInfo> rateInfos = new HashSet<>();
        try {
            cleanupRegistry(subscriptionName, new ArrayList<>(currentConsumers));

            for (String consumerId : currentConsumers) {
                RateHistory history = readOrCreateRateHistory(subscriptionName, consumerId);
                Optional<MaxRate> maxRate = readMaxRate(subscriptionName, consumerId);
                rateInfos.add(new ConsumerRateInfo(consumerId, maxRate, history));
            }
        } catch (Exception e) {
            throw new InternalProcessingException("Trouble creating rate entries in zookeeper", e);
        }
        return rateInfos;
    }

    void update(SubscriptionName subscriptionName, Map<String, MaxRate> newMaxRates) {
        try {
            for (Map.Entry<String, MaxRate> entry : newMaxRates.entrySet()) {
                String maxRatePath = zookeeperPaths.consumersMaxRatePath(subscriptionName, entry.getKey());
                writeOrCreate(maxRatePath, objectMapper.writeValueAsBytes(entry.getValue()));
            }
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    Optional<MaxRate> readMaxRate(SubscriptionName subscriptionName, String consumerId) {
        String path = zookeeperPaths.consumersMaxRatePath(subscriptionName, consumerId);
        try {
            byte[] serialized = curator.getData().forPath(path);
            return Optional.of(objectMapper.readValue(serialized, MaxRate.class));
        } catch (KeeperException.NoNodeException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    RateHistory readOrCreateRateHistory(SubscriptionName subscriptionName, String consumerId) {
        String path = zookeeperPaths.consumersRateHistoryPath(subscriptionName, consumerId);
        try {
            byte[] serialized = curator.getData().forPath(path);
            return objectMapper.readValue(serialized, RateHistory.class);
        } catch (KeeperException.NoNodeException e) {
            RateHistory empty = RateHistory.empty();
            writeRateHistory(subscriptionName, consumerId, empty);
            return empty;
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    void writeRateHistory(SubscriptionName subscriptionName, String consumerId, RateHistory rateHistory) {
        String path = zookeeperPaths.consumersRateHistoryPath(subscriptionName, consumerId);
        try {
            byte[] serialized = objectMapper.writeValueAsBytes(rateHistory);
            writeOrCreate(path, serialized);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private void cleanupRegistry(SubscriptionName subscriptionName, List<String> currentConsumers) throws Exception {
        String basePath = zookeeperPaths.consumersRateRuntimePath(subscriptionName);

        try {
            curator.create().creatingParentContainersIfNeeded().forPath(basePath);
        } catch (KeeperException.NodeExistsException e) {
            // ignore
        }

        List<String> previousConsumers = curator.getChildren().forPath(basePath);

        List<String> toRemove = ListUtils.subtract(previousConsumers, currentConsumers);
        List<String> toAdd = ListUtils.subtract(currentConsumers, previousConsumers);

        if (!toRemove.isEmpty()) {
            logger.info("Removing consumers for max rates for subscription {}: {}", subscriptionName, toRemove);
        }
        if (!toAdd.isEmpty()) {
            logger.info("Adding consumers for max rates for subscription {}: {}", subscriptionName, toAdd);
        }

        toRemove.forEach(removedConsumer -> removeConsumerEntries(subscriptionName, removedConsumer));
        toAdd.forEach(addedConsumer -> createEmptyHistory(subscriptionName, addedConsumer));
    }

    private void removeConsumerEntries(SubscriptionName subscriptionName, String consumerId) {
        try {
            curator.delete().deletingChildrenIfNeeded()
                    .forPath(zookeeperPaths.consumersRatePath(subscriptionName, consumerId));
        } catch (KeeperException.NoNodeException e) {
            // ignore
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private void createEmptyHistory(SubscriptionName subscriptionName, String consumerId) {
        writeRateHistory(subscriptionName, consumerId, RateHistory.empty());
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
}
