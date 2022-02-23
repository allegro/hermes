package pl.allegro.tech.hermes.frontend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.frontend.blacklist.BlacklistZookeeperNotifyingCache;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessagesLoader;
import pl.allegro.tech.hermes.frontend.cache.topic.NotificationBasedTopicsCache;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.services.HealthCheckService;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.frontend.validator.TopicMessageValidator;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperDatacenterReadinessRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import javax.inject.Named;
import java.time.Clock;
import java.util.List;

@Configuration
public class FrontendConfiguration {

    @Bean
    public MessageValidators messageValidators(List<TopicMessageValidator> messageValidators) {
        return new MessageValidators(messageValidators);
    }

    @Bean(initMethod = "start")
    public TopicsCache notificationBasedTopicsCache(InternalNotificationsBus internalNotificationsBus,
                                                    GroupRepository groupRepository,
                                                    TopicRepository topicRepository,
                                                    HermesMetrics hermesMetrics,
                                                    KafkaNamesMapper kafkaNamesMapper,
                                                    BlacklistZookeeperNotifyingCache blacklistZookeeperNotifyingCache) {

        return new NotificationBasedTopicsCache(internalNotificationsBus, blacklistZookeeperNotifyingCache,
                groupRepository, topicRepository, hermesMetrics, kafkaNamesMapper);
    }

    @Bean
    public BackupMessagesLoader backupMessagesLoader(BrokerMessageProducer brokerMessageProducer,
                                                     BrokerListeners brokerListeners,
                                                     TopicsCache topicsCache,
                                                     Trackers trackers,
                                                     ConfigFactory config) {
        return new BackupMessagesLoader(brokerMessageProducer, brokerListeners, topicsCache, trackers, config);
    }

    @Bean(initMethod = "extend")
    public PersistentBufferExtension persistentBufferExtension(ConfigFactory configFactory,
                                                               Clock clock,
                                                               BrokerListeners listeners,
                                                               BackupMessagesLoader backupMessagesLoader,
                                                               HermesMetrics hermesMetrics) {
        return new PersistentBufferExtension(configFactory, clock, listeners, backupMessagesLoader,
                hermesMetrics);
    }

    @Bean(initMethod = "startup")
    public BlacklistZookeeperNotifyingCache blacklistZookeeperNotifyingCache(@Named(CuratorType.HERMES) CuratorFramework curator,
                                                                             ZookeeperPaths zookeeperPaths) {
        return new BlacklistZookeeperNotifyingCache(curator, zookeeperPaths);
    }

    @Bean(destroyMethod = "close")
    public ReadinessRepository zookeeperDatacenterReadinessRepository(@Named(CuratorType.HERMES) CuratorFramework zookeeper,
                                                                      ZookeeperPaths paths,
                                                                      ObjectMapper mapper) {
        return new ZookeeperDatacenterReadinessRepository(zookeeper, mapper, paths);
    }

    @Bean(initMethod = "startup")
    public HealthCheckService healthCheckService() {
        return new HealthCheckService();
    }

    @Bean
    public BrokerListeners defaultBrokerListeners() {
        return new BrokerListeners();
    }

    @Bean
    @Named("moduleName")
    public String moduleName() {
        return "producer";
    }
}
