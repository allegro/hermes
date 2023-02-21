package pl.allegro.tech.hermes.management.infrastructure.monitoring;

import com.google.common.collect.Lists;
import org.apache.kafka.clients.admin.AdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.management.config.MonitoringProperties;
import pl.allegro.tech.hermes.management.config.kafka.AdminClientFactory;
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.config.kafka.KafkaNamesMappers;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

import java.time.Duration;
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

    private final KafkaClustersProperties kafkaClustersProperties;
    private final KafkaNamesMappers kafkaNamesMappers;
    private final MonitoringProperties monitoringProperties;
    private final SubscriptionService subscriptionService;
    private final TopicService topicService;

    private volatile List<TopicAndSubscription> readOnlySubscriptionsWithUnassignedPartitionsCache = new ArrayList<>();

    public MonitoringCache(KafkaClustersProperties kafkaClustersProperties, KafkaNamesMappers kafkaNamesMappers,
                           MonitoringProperties monitoringProperties, SubscriptionService subscriptionService, TopicService topicService) {
        this.kafkaClustersProperties = kafkaClustersProperties;
        this.kafkaNamesMappers = kafkaNamesMappers;
        this.monitoringProperties = monitoringProperties;
        this.subscriptionService = subscriptionService;
        this.topicService = topicService;
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
        List<List<TopicAndSubscription>> splitSubscriptions = getSplitActiveSubscriptions();

        ExecutorService executorService = Executors.newFixedThreadPool(monitoringProperties.getNumberOfThreads());

        List<Future<List<TopicAndSubscription>>> futures = splitSubscriptions.stream().map(it -> executorService.submit(() -> {
            try {
                List<MonitoringService> monitoringServices = createMonitoringService();
                logger.info("Monitoring started for {} subscriptions", it.size());
                return checkIfAllPartitionsAreAssignedToSubscriptions(it, monitoringServices);
            } catch (Exception e) {
                logger.error("Error in monitoring: ", e);
                return new ArrayList<TopicAndSubscription>();
            } finally {
                logger.info("Monitoring ended");
                executorService.shutdown();
            }
        })).collect(toList());

        waitForAllThreadsToEnd(futures);
        updateSubscriptionsWithUnassignedPartitionsCache(futures);
    }

    private void updateSubscriptionsWithUnassignedPartitionsCache(List<Future<List<TopicAndSubscription>>> futures) {
        readOnlySubscriptionsWithUnassignedPartitionsCache = futures.stream().map(future -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).flatMap(List::stream).collect(toList());
    }

    private static void waitForAllThreadsToEnd(List<Future<List<TopicAndSubscription>>> futures) {
        while (futures.stream().anyMatch(future -> !future.isDone())) {
            try {
                Thread.sleep(Duration.ofSeconds(1).toMillis());
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private List<List<TopicAndSubscription>> getSplitActiveSubscriptions() {
        return splitSubscriptions(getAllActiveSubscriptions());
    }

    private List<TopicAndSubscription> checkIfAllPartitionsAreAssignedToSubscriptions(List<TopicAndSubscription> subscriptions,
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

    private List<List<TopicAndSubscription>> splitSubscriptions(List<TopicAndSubscription> topicAndSubscriptions) {
        return Lists.partition(topicAndSubscriptions, (topicAndSubscriptions.size() / monitoringProperties.getNumberOfThreads()) + 1);
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
        List<TopicAndSubscription> topicAndSubscriptions = new ArrayList<>();
        subscriptions.forEach(subscription -> topicAndSubscriptions.add(new TopicAndSubscription(topic, subscription)));
        return topicAndSubscriptions;
    }

    public List<MonitoringService> createMonitoringService() {
        return kafkaClustersProperties.getClusters().stream().map(kafkaProperties -> {
            KafkaNamesMapper kafkaNamesMapper = kafkaNamesMappers.getMapper(kafkaProperties.getQualifiedClusterName());
            AdminClient brokerAdminClient = AdminClientFactory.brokerAdminClient(kafkaProperties);

            return new MonitoringService(
                    kafkaNamesMapper,
                    brokerAdminClient,
                    kafkaProperties.getQualifiedClusterName());
        }).collect(toList());
    }
}