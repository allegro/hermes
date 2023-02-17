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
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.config.kafka.KafkaNamesMappers;
import pl.allegro.tech.hermes.management.config.kafka.KafkaProperties;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

public class MonitoringCache {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringCache.class);

    private final KafkaClustersProperties kafkaClustersProperties;
    private final KafkaNamesMappers kafkaNamesMappers;
    private final MonitoringProperties monitoringProperties;
    private final SubscriptionService subscriptionService;
    private final TopicService topicService;

    private volatile List<List<TopicAndSubscription>> readySubscriptionsWithUnassignedPartitionsCache = new ArrayList<>();
    private volatile List<List<TopicAndSubscription>> unreadySubscriptionsWithUnassignedPartitionsCache = new ArrayList<>();

    public MonitoringCache(KafkaClustersProperties kafkaClustersProperties, KafkaNamesMappers kafkaNamesMappers,
                           MonitoringProperties monitoringProperties, SubscriptionService subscriptionService, TopicService topicService) {
        this.kafkaClustersProperties = kafkaClustersProperties;
        this.kafkaNamesMappers = kafkaNamesMappers;
        this.monitoringProperties = monitoringProperties;
        this.subscriptionService = subscriptionService;
        this.topicService = topicService;
        if (monitoringProperties.isEnabled()) {
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(this::checkSubscriptionsPartitions, 0,
                    monitoringProperties.getScanEvery().getSeconds(), TimeUnit.SECONDS);
        }
    }

    public List<TopicAndSubscription> getSubscriptionsWithUnassignedPartitions() {
        return readySubscriptionsWithUnassignedPartitionsCache.stream().flatMap(List::stream).collect(toList());
    }

    private void checkSubscriptionsPartitions() {
        readySubscriptionsWithUnassignedPartitionsCache = unreadySubscriptionsWithUnassignedPartitionsCache;
        unreadySubscriptionsWithUnassignedPartitionsCache.clear();
        monitorSubscriptionsPartitions();
    }

    private void monitorSubscriptionsPartitions() {
        List<List<TopicAndSubscription>> splitSubscriptions = getSplitActiveSubscriptions();

        ExecutorService executorService = Executors.newFixedThreadPool(monitoringProperties.getNumberOfThreads());

        for (int thread = 0; thread < monitoringProperties.getNumberOfThreads(); thread++) {
            int part = thread;
            executorService.submit(() -> {
                try {
                    List<MonitoringService> monitoringServices = createMonitoringService();
                    logger.info("Monitoring {} started for {} subscriptions", part, splitSubscriptions.get(part).size());
                    checkIfAllPartitionsAreAssignedToSubscriptions(splitSubscriptions.get(part), monitoringServices);
                } catch (Exception e) {
                    logger.error("Error in monitoring: ", e);
                } finally {
                    logger.info("Monitoring {} ended", part);
                    executorService.shutdown();
                }
            });
        }
    }

    private List<List<TopicAndSubscription>> getSplitActiveSubscriptions() {
        return splitSubscriptions(getAllActiveSubscriptions());
    }

    private void checkIfAllPartitionsAreAssignedToSubscriptions(List<TopicAndSubscription> subscriptions,
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
        addSubscriptionsToCache(subscriptionsWithUnassignedPartitions);
    }

    private void addSubscriptionsToCache(List<TopicAndSubscription> subscriptionsWithUnassignedPartitions) {
        unreadySubscriptionsWithUnassignedPartitionsCache.add(subscriptionsWithUnassignedPartitions);
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
            AdminClient brokerAdminClient = brokerAdminClient(kafkaProperties);

            return new MonitoringService(
                    kafkaNamesMapper,
                    brokerAdminClient,
                    kafkaProperties.getQualifiedClusterName());
        }).collect(toList());
    }

    private AdminClient brokerAdminClient(KafkaProperties kafkaProperties) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapKafkaServer());
        props.put(REQUEST_TIMEOUT_MS_CONFIG, kafkaProperties.getKafkaServerRequestTimeoutMillis());
        props.put(SECURITY_PROTOCOL_CONFIG, DEFAULT_SECURITY_PROTOCOL);
        if (kafkaProperties.getSasl().isEnabled()) {
            props.put(SASL_MECHANISM, kafkaProperties.getSasl().getMechanism());
            props.put(SECURITY_PROTOCOL_CONFIG, kafkaProperties.getSasl().getProtocol());
            props.put(SASL_JAAS_CONFIG, kafkaProperties.getSasl().getJaasConfig());
        }
        return AdminClient.create(props);
    }
}