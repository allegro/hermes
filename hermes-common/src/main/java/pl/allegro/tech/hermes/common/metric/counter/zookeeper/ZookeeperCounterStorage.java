package pl.allegro.tech.hermes.common.metric.counter.zookeeper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.Metrics;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.MetricsDeltaCalculator;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionNotExistsException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;

import javax.inject.Inject;

public class ZookeeperCounterStorage implements CounterStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperCounterStorage.class);

    private final MetricsDeltaCalculator deltaCalculator = new MetricsDeltaCalculator();

    private final SharedCounter sharedCounter;

    private final DistributedEphemeralCounter distributedCounter;
    private final SubscriptionRepository subscriptionRepository;
    private final ZookeeperPaths zookeeperPaths;

    @Inject
    public ZookeeperCounterStorage(SharedCounter sharedCounter,
            DistributedEphemeralCounter distributedCounter,
            ConfigFactory configFactory,
            SubscriptionRepository subscriptionRepository) {
        this.sharedCounter = sharedCounter;
        this.distributedCounter = distributedCounter;
        this.subscriptionRepository = subscriptionRepository;

        this.zookeeperPaths = new ZookeeperPaths(configFactory.getStringProperty(Configs.ZOOKEEPER_ROOT));
    }

    @Override
    public void setTopicCounter(TopicName topicName, Metrics.Counter counter, long count) {
        incrementSharedCounter(topicMetricPath(topicName, counter), count);
    }

    @Override
    public void setSubscriptionCounter(TopicName topicName, String subscriptionName, Metrics.Counter counter, long count) {
        try {
            subscriptionRepository.ensureSubscriptionExists(topicName, subscriptionName);
            incrementSharedCounter(subscriptionMetricPath(topicName, subscriptionName, counter), count);
        } catch (SubscriptionNotExistsException e) {
            LOGGER.debug("Trying to report metric on not existing subscription {} {}", topicName, subscriptionName);
        }
    }

    @Override
    public void setInflightCounter(String hostname, TopicName topicName, String subscriptionName, long count) {
        distributedCounter.setCounterValue(
                zookeeperPaths.inflightPath(hostname, topicName, subscriptionName, Metrics.Counter.CONSUMER_INFLIGHT.normalizedName()),
                count
        );
    }

    @Override
    public long getTopicCounter(TopicName topicName, Metrics.Counter counter) {
        return sharedCounter.getValue(topicMetricPath(topicName, counter));
    }

    @Override
    public long getSubscriptionCounter(TopicName topicName, String subscriptionName, Metrics.Counter counter) {
        return sharedCounter.getValue(subscriptionMetricPath(topicName, subscriptionName, counter));
    }

    @Override
    public long getInflightCounter(TopicName topicName, String subscriptionName) {
        return distributedCounter.getValue(
            zookeeperPaths.consumersPath(),
            zookeeperPaths.subscriptionMetricPathWithoutBasePath(
                topicName, subscriptionName, Metrics.Counter.CONSUMER_INFLIGHT.normalizedName()
            )
        );
    }

    @Override
    public int countInflightNodes(TopicName topicName, String subscriptionName) {
        return distributedCounter.countOccurrences(
            zookeeperPaths.consumersPath(),
            zookeeperPaths.subscriptionMetricPathWithoutBasePath(
                topicName, subscriptionName, Metrics.Counter.CONSUMER_INFLIGHT.normalizedName()
            )
        );
    }

    private void incrementSharedCounter(String metricPath, long count) {
        long delta = deltaCalculator.calculateDelta(metricPath, count);

        if (delta != 0 && !sharedCounter.increment(metricPath, delta)) {
            deltaCalculator.revertDelta(metricPath, delta);
        }
    }

    private String topicMetricPath(TopicName topicName, Metrics.Counter counter) {
        return zookeeperPaths.topicMetricPath(topicName, counter.normalizedName());
    }

    private String subscriptionMetricPath(TopicName topicName, String subscriptionName, Metrics.Counter counter) {
        return zookeeperPaths.subscriptionMetricPath(topicName, subscriptionName, counter.normalizedName());
    }

}
