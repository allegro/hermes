package pl.allegro.tech.hermes.frontend.config;

import jakarta.inject.Named;
import java.time.Clock;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.frontend.blacklist.BlacklistZookeeperNotifyingCache;
import pl.allegro.tech.hermes.frontend.buffer.BackupMessagesLoader;
import pl.allegro.tech.hermes.frontend.buffer.PersistentBufferExtension;
import pl.allegro.tech.hermes.frontend.cache.topic.NotificationBasedTopicsCache;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.metric.ThroughputRegistry;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.validator.MessageValidators;
import pl.allegro.tech.hermes.frontend.validator.TopicMessageValidator;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.schema.SchemaExistenceEnsurer;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

@Configuration
@EnableConfigurationProperties(LocalMessageStorageProperties.class)
public class FrontendConfiguration {

  @Bean
  public MessageValidators messageValidators(List<TopicMessageValidator> messageValidators) {
    return new MessageValidators(messageValidators);
  }

  @Bean(initMethod = "start")
  public TopicsCache notificationBasedTopicsCache(
      InternalNotificationsBus internalNotificationsBus,
      GroupRepository groupRepository,
      TopicRepository topicRepository,
      MetricsFacade metricsFacade,
      ThroughputRegistry throughputRegistry,
      KafkaNamesMapper kafkaNamesMapper,
      BlacklistZookeeperNotifyingCache blacklistZookeeperNotifyingCache) {

    return new NotificationBasedTopicsCache(
        internalNotificationsBus,
        blacklistZookeeperNotifyingCache,
        groupRepository,
        topicRepository,
        metricsFacade,
        throughputRegistry,
        kafkaNamesMapper);
  }

  @Bean
  public BackupMessagesLoader backupMessagesLoader(
      @Named("localDatacenterBrokerProducer") BrokerMessageProducer brokerMessageProducer,
      BrokerListeners brokerListeners,
      TopicsCache topicsCache,
      SchemaRepository schemaRepository,
      Trackers trackers,
      LocalMessageStorageProperties localMessageStorageProperties) {
    return new BackupMessagesLoader(
        brokerMessageProducer,
        brokerMessageProducer,
        brokerListeners,
        topicsCache,
        schemaRepository,
        new SchemaExistenceEnsurer(schemaRepository),
        trackers,
        localMessageStorageProperties);
  }

  @Bean(initMethod = "extend")
  public PersistentBufferExtension persistentBufferExtension(
      LocalMessageStorageProperties localMessageStorageProperties,
      Clock clock,
      BrokerListeners listeners,
      BackupMessagesLoader backupMessagesLoader,
      MetricsFacade metricsFacade) {
    return new PersistentBufferExtension(
        localMessageStorageProperties, clock, listeners, backupMessagesLoader, metricsFacade);
  }

  @Bean(initMethod = "startup")
  public BlacklistZookeeperNotifyingCache blacklistZookeeperNotifyingCache(
      CuratorFramework curator, ZookeeperPaths zookeeperPaths) {
    return new BlacklistZookeeperNotifyingCache(curator, zookeeperPaths);
  }

  @Bean
  public BrokerListeners defaultBrokerListeners() {
    return new BrokerListeners();
  }

  @Bean
  public String moduleName() {
    return "producer";
  }
}
