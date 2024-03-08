package pl.allegro.tech.hermes.management.infrastructure.monitoring;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicAndSubscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.config.MonitoringProperties;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

public class MonitoringCache {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringCache.class);

    private final MonitoringProperties monitoringProperties;
    private final SubscriptionService subscriptionService;
    private final TopicService topicService;
    private final MonitoringServicesCreator monitoringServicesCreator;

    private volatile List<TopicAndSubscription> readOnlySubscriptionsWithUnassignedPartitionsCache = new ArrayList<>();

    public MonitoringCache(MonitoringProperties monitoringProperties, SubscriptionService subscriptionService,
                           TopicService topicService, MonitoringServicesCreator monitoringServicesCreator) {
        this.monitoringProperties = monitoringProperties;
        this.subscriptionService = subscriptionService;
        this.topicService = topicService;
        this.monitoringServicesCreator = monitoringServicesCreator;
        if (monitoringProperties.isEnabled()) {
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(this::monitorSubscriptionsPartitions, 0,
                    monitoringProperties.getScanEvery().getSeconds(), TimeUnit.SECONDS);
        }
    }

    public List<TopicAndSubscription> getSubscriptionsWithUnassignedPartitions() {
        return readOnlySubscriptionsWithUnassignedPartitionsCache;
    }

    private void monitorSubscriptionsPartitions() {
        List<List<TopicAndSubscription>> subscriptionsPartitioned = getPartitionedActiveSubscriptions();

        List<TopicAndSubscription> subscriptionsWithUnassignedPartitions =
                getFreshSubscriptionsWithUnassignedPartitions(subscriptionsPartitioned);
        updateSubscriptionsWithUnassignedPartitionsCache(subscriptionsWithUnassignedPartitions);
    }

    private List<TopicAndSubscription> getFreshSubscriptionsWithUnassignedPartitions(
            List<List<TopicAndSubscription>> splitSubscriptions) {
        ExecutorService executorService = Executors.newFixedThreadPool(monitoringProperties.getNumberOfThreads());

        List<Future<List<TopicAndSubscription>>> futures = splitSubscriptions.stream().map(it -> executorService.submit(() -> {
            try {
                List<MonitoringService> monitoringServices = monitoringServicesCreator.createMonitoringServices();
                logger.info("Monitoring started for {} subscriptions", it.size());
                return findSubscriptionsWithUnassignedPartitions(it, monitoringServices);
            } catch (Exception e) {
                logger.error("Error in monitoring: ", e);
                return new ArrayList<TopicAndSubscription>();
            } finally {
                logger.info("Monitoring ended");
                executorService.shutdown();
            }
        })).collect(toList());

        waitForAllThreadsToEnd(futures);
        return getSubscriptionsWithUnassignedPartitionsFromFutures(futures);
    }

    private List<TopicAndSubscription> getSubscriptionsWithUnassignedPartitionsFromFutures(
                List<Future<List<TopicAndSubscription>>> futures) {
        return futures.stream().map(future -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new MonitoringCacheException("Error during monitoring subscriptions - cannot get data from future", e);
            }
        }).flatMap(List::stream).collect(toList());
    }

    private void updateSubscriptionsWithUnassignedPartitionsCache(List<TopicAndSubscription> subscriptionsWithUnassignedPartitions) {
        readOnlySubscriptionsWithUnassignedPartitionsCache = subscriptionsWithUnassignedPartitions;
    }

    private static void waitForAllThreadsToEnd(List<Future<List<TopicAndSubscription>>> futures) {
        try {
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (ExecutionException | InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private List<List<TopicAndSubscription>> getPartitionedActiveSubscriptions() {
        return partitionSubscriptions(getAllActiveSubscriptions());
    }

    private List<List<TopicAndSubscription>> partitionSubscriptions(List<TopicAndSubscription> topicAndSubscriptions) {
        return Lists.partition(topicAndSubscriptions, (topicAndSubscriptions.size() / monitoringProperties.getNumberOfThreads()) + 1);
    }

    private List<TopicAndSubscription> findSubscriptionsWithUnassignedPartitions(List<TopicAndSubscription> subscriptions,
                                                                List<MonitoringService> monitoringServices) {
        List<TopicAndSubscription> subscriptionsWithUnassignedPartitions = new ArrayList<>();

        subscriptions.forEach(topicSubscription ->
            monitoringServices.forEach(monitoringService -> {
                    if (!monitoringService.checkIfAllPartitionsAreAssigned(topicSubscription.getTopic(),
                            topicSubscription.getSubscription())) {
                        subscriptionsWithUnassignedPartitions.add(topicSubscription);
                    }
                }
            ));
        return subscriptionsWithUnassignedPartitions;
    }

    private List<TopicAndSubscription> getAllActiveSubscriptions() {
        List<TopicAndSubscription> subscriptions = new ArrayList<>();
        topicService.listQualifiedTopicNames().forEach(topic -> {
            TopicName topicName = fromQualifiedName(topic);
            List<Subscription> topicSubscriptions = subscriptionService.listSubscriptions(topicName);
            subscriptions.addAll(createTopicSubscriptions(topicService.getTopicDetails(topicName),
                    topicSubscriptions.stream().filter(subscription -> subscription.getState().equals(Subscription.State.ACTIVE))
                            .map(Subscription::getName).collect(Collectors.toList())));
        });
        return subscriptions;
    }

    private List<TopicAndSubscription> createTopicSubscriptions(Topic topic, List<String> subscriptions) {
        return subscriptions.stream().map(it -> new TopicAndSubscription(topic, it)).collect(toList());
    }
}