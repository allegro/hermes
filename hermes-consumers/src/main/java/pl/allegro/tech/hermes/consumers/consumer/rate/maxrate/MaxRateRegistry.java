package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.ListUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
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

    public Set<ConsumerRateInfo> ensureCorrectAssignments(Subscription subscription, Set<String> currentConsumers) {
        Set<ConsumerRateInfo> rateInfos = new HashSet<>();
        try {
            cleanupRegistry(subscription, new ArrayList<>(currentConsumers));

            for (String consumerId : currentConsumers) {
                RateHistory history = readOrCreateRateHistory(subscription, consumerId);
                Optional<MaxRate> maxRate = readMaxRate(subscription, consumerId);
                rateInfos.add(new ConsumerRateInfo(consumerId, maxRate, history));
            }
        } catch (Exception e) {
            throw new InternalProcessingException("Trouble creating rate entries in zookeeper", e);
        }
        return rateInfos;
    }

    public void update(Subscription subscription, Map<String, MaxRate> newMaxRates) {
        try {
            for (Map.Entry<String, MaxRate> entry : newMaxRates.entrySet()) {
                String maxRatePath = zookeeperPaths.consumersMaxRatePath(subscription.getName(), entry.getKey());
                    writeOrCreate(maxRatePath, objectMapper.writeValueAsBytes(entry.getValue()));
            }
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private void cleanupRegistry(Subscription subscription, List<String> currentConsumers) throws Exception {
        String basePath = zookeeperPaths.consumersRateRuntimePath() + "/" + subscription;

        try {
            curator.create().creatingParentContainersIfNeeded().forPath(basePath);
        } catch (KeeperException.NodeExistsException e) {
            // ignore
        }

        List<String> previousConsumers = curator.getChildren().forPath(basePath);

        List<String> toRemove = ListUtils.subtract(previousConsumers, currentConsumers);
        List<String> toAdd = ListUtils.subtract(currentConsumers, previousConsumers);

        toRemove.forEach(removedConsumer -> removeConsumerEntries(subscription, removedConsumer));
        toAdd.forEach(addedConsumer -> createEmptyHistory(subscription, addedConsumer));
    }

    private void removeConsumerEntries(Subscription subscription, String consumerId) {
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(zookeeperPaths.consumersRatePath(subscription.getName(), consumerId));
        } catch (KeeperException.NoNodeException e) {
            // ignore
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private void createEmptyHistory(Subscription subscription, String consumerId) {
        writeRateHistory(subscription, consumerId, RateHistory.empty());
    }

    public Optional<MaxRate> readMaxRate(Subscription subscription, String consumerId) {
        String path = zookeeperPaths.consumersMaxRatePath(subscription.getName(), consumerId);
        try {
            byte[] serialized = curator.getData().forPath(path);
            return Optional.of(objectMapper.readValue(serialized, MaxRate.class));
        } catch (KeeperException.NoNodeException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public RateHistory readOrCreateRateHistory(Subscription subscription, String consumerId) {
        String path = zookeeperPaths.consumersRateHistoryPath(subscription.getName(), consumerId);
        try {
            byte[] serialized = curator.getData().forPath(path);
            return objectMapper.readValue(serialized, RateHistory.class);
        } catch (KeeperException.NoNodeException e) {
            RateHistory empty = RateHistory.empty();
            writeRateHistory(subscription, consumerId, empty);
            return empty;
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    public void writeRateHistory(Subscription subscription, String consumerId, RateHistory rateHistory) {
        String path = zookeeperPaths.consumersRateHistoryPath(subscription.getName(), consumerId);
        try {
            byte[] serialized = objectMapper.writeValueAsBytes(rateHistory);
            writeOrCreate(path, serialized);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private void writeOrCreate(String path, byte[] serializedData) throws Exception {
        try {
            curator.create().creatingParentContainersIfNeeded().forPath(path, serializedData);
        } catch (KeeperException.NodeExistsException e) {
            curator.setData().forPath(path, serializedData);
        }
    }
}
