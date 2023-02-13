package pl.allegro.tech.hermes.management.infrastructure.monitoring;

import org.apache.kafka.clients.admin.AdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
import java.util.Collections;
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

@Component
public class MonitoringCache {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringCache.class);

    private final KafkaClustersProperties kafkaClustersProperties;
    private final KafkaNamesMappers kafkaNamesMappers;
    private final MonitoringProperties monitoringProperties;
    private final SubscriptionService subscriptionService;
    private final TopicService topicService;

    private volatile Integer usingCache = 1;
    private volatile List<List<TopicSubscription>> subscriptionsWithUnassignedPartitions1 = new ArrayList<>();
    private volatile List<List<TopicSubscription>> subscriptionsWithUnassignedPartitions2 = new ArrayList<>();

    @Autowired
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
                    monitoringProperties.getSecondsBetweenScans(), TimeUnit.SECONDS);
        }
    }

    public List<TopicSubscription> getSubscriptionsWithUnassignedPartitions() {
        if (usingCache == 1) {
            return subscriptionsWithUnassignedPartitions2.stream().flatMap(List::stream).collect(toList());
        } else {
            return subscriptionsWithUnassignedPartitions1.stream().flatMap(List::stream).collect(toList());
        }
    }

    private void checkSubscriptionsPartitions() {
        if (usingCache == 1) {
            subscriptionsWithUnassignedPartitions2.clear();
            usingCache = 2;
        } else {
            subscriptionsWithUnassignedPartitions1.clear();
            usingCache = 1;
        }
        monitorSubscriptionsPartitions();
    }

    private void monitorSubscriptionsPartitions() {
        List<TopicSubscription> topicSubscriptions = getAllActiveSubscriptions();
        List<List<TopicSubscription>> splitSubscriptions = splitSubscriptions(topicSubscriptions);

        ExecutorService executorService = Executors.newFixedThreadPool(monitoringProperties.getNumberOfThreads());

        for (int thread = 0; thread < monitoringProperties.getNumberOfThreads(); thread++) {
            int part = thread;
            executorService.submit(() -> {
                try {
                    List<MonitoringService> monitoringServices = createMonitoringService();
                    List<TopicSubscription> subscriptionsWithUnassignedPartitions = new ArrayList<>();
                    logger.info("Monitoring {} started for {} subscriptions", part, splitSubscriptions.get(part).size());

                    splitSubscriptions.get(part).forEach(topicSubscription ->
                            monitoringServices.forEach(monitoringService -> {
                                        if (!monitoringService.checkIfAllPartitionsAreAssigned(topicSubscription.getTopic(),
                                                topicSubscription.getSubscription())) {
                                            subscriptionsWithUnassignedPartitions.add(topicSubscription);
                                        }
                                    }
                            ));
                    addSubscriptionsToCache(subscriptionsWithUnassignedPartitions);
                } catch (Exception e) {
                    logger.error("Error in monitoring: ", e);
                } finally {
                    logger.info("Monitoring {} ended", part);
                    executorService.shutdown();
                }
            });
        }
    }

    private void addSubscriptionsToCache(List<TopicSubscription> subscriptionsWithUnassignedPartitions) {
        if (usingCache == 1) {
            subscriptionsWithUnassignedPartitions1.add(subscriptionsWithUnassignedPartitions);
        } else {
            subscriptionsWithUnassignedPartitions2.add(subscriptionsWithUnassignedPartitions);
        }
    }

    private List<List<TopicSubscription>> splitSubscriptions(List<TopicSubscription> topicSubscriptions) {
        List<List<TopicSubscription>> splitSubscriptions = new ArrayList<>(Collections.emptyList());
        splitSubscriptions.add(topicSubscriptions.subList(0, topicSubscriptions.size() / monitoringProperties.getNumberOfThreads()));
        for (int i = 1; i < monitoringProperties.getNumberOfThreads(); i++) {
            splitSubscriptions.add(topicSubscriptions.subList(topicSubscriptions.size() * i / monitoringProperties.getNumberOfThreads(),
                    topicSubscriptions.size() * (i + 1) / monitoringProperties.getNumberOfThreads()));
        }
        return splitSubscriptions;
    }

    private List<TopicSubscription> getAllActiveSubscriptions() {
        List<TopicSubscription> subscriptions = new ArrayList<>();
        topicService.listQualifiedTopicNames().forEach(topic -> {
            TopicName topicName = fromQualifiedName(topic);
            List<Subscription> topicSubscriptions = subscriptionService.listSubscriptions(topicName);
            subscriptions.addAll(createTopicSubscriptions(topicService.getTopicDetails(topicName),
                    topicSubscriptions.stream().filter(subscription -> subscription.getState().equals(Subscription.State.ACTIVE))
                            .map(Subscription::getName).collect(Collectors.toList())));
        });
        return subscriptions;
    }

    private List<TopicSubscription> createTopicSubscriptions(Topic topic, List<String> subscriptions) {
        List<TopicSubscription> topicSubscriptions = new ArrayList<>();
        subscriptions.forEach(subscription -> topicSubscriptions.add(new TopicSubscription(topic, subscription)));
        return topicSubscriptions;
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