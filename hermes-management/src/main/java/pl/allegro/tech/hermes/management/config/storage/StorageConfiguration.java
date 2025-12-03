package pl.allegro.tech.hermes.management.config.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import pl.allegro.tech.hermes.common.di.factories.ModelAwareZookeeperNotifyingCacheFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.domain.CredentialsRepository;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.dc.DcNameSource;
import pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.dc.EnvironmentVariableDatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperCredentialsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperMessagePreviewRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperOAuthProviderRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperWorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.cache.ModelAwareZookeeperNotifyingCache;
import pl.allegro.tech.hermes.infrastructure.zookeeper.notifications.ZookeeperInternalNotificationBus;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsRepository;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;
import pl.allegro.tech.hermes.management.domain.readiness.DatacenterReadinessRepository;
import pl.allegro.tech.hermes.management.domain.retransmit.OfflineRetransmissionRepository;
import pl.allegro.tech.hermes.management.infrastructure.detection.ZookeeperInactiveTopicsRepository;
import pl.allegro.tech.hermes.management.infrastructure.metrics.SummedSharedCounter;
import pl.allegro.tech.hermes.management.infrastructure.readiness.ZookeeperDatacenterReadinessRepository;
import pl.allegro.tech.hermes.management.infrastructure.retransmit.ZookeeperOfflineRetransmissionRepository;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperRepositoryManager;

@Configuration
@EnableConfigurationProperties(StorageClustersProperties.class)
public class StorageConfiguration {

  @Autowired StorageClustersProperties storageClustersProperties;

  @Autowired ObjectMapper objectMapper;

  @Bean
  DatacenterNameProvider dcNameProvider() {
    if (storageClustersProperties.getDatacenterNameSource() == DcNameSource.ENV) {
      return new EnvironmentVariableDatacenterNameProvider(
          storageClustersProperties.getDatacenterNameSourceEnv());
    } else {
      return new DefaultDatacenterNameProvider();
    }
  }

  @Bean(initMethod = "start", destroyMethod = "stop")
  ZookeeperClientManager clientManager() {
    return new ZookeeperClientManager(storageClustersProperties, dcNameProvider());
  }

  @Bean
  ZookeeperGroupRepositoryFactory zookeeperGroupRepositoryFactory() {
    return new DefaultZookeeperGroupRepositoryFactory();
  }

  @Bean(initMethod = "start")
  ZookeeperRepositoryManager repositoryManager(
      ZookeeperGroupRepositoryFactory zookeeperGroupRepositoryFactory) {
    return new ZookeeperRepositoryManager(
        clientManager(),
        dcNameProvider(),
        objectMapper,
        zookeeperPaths(),
        zookeeperGroupRepositoryFactory);
  }

  @Bean
  ZookeeperPaths zookeeperPaths() {
    return new ZookeeperPaths(storageClustersProperties.getPathPrefix());
  }

  @Bean
  MultiDatacenterRepositoryCommandExecutor multiDcRepositoryCommandExecutor(
      ZookeeperGroupRepositoryFactory zookeeperGroupRepositoryFactory, ModeService modeService) {
    return new MultiDatacenterRepositoryCommandExecutor(
        repositoryManager(zookeeperGroupRepositoryFactory),
        storageClustersProperties.isTransactional(),
        modeService);
  }

  @Bean
  SummedSharedCounter summedSharedCounter(ZookeeperClientManager manager) {
    return new SummedSharedCounter(
        manager.getClients(),
        storageClustersProperties.getSharedCountersExpiration(),
        storageClustersProperties.getRetrySleep(),
        storageClustersProperties.getRetryTimes());
  }

  @Bean
  GroupRepository groupRepository() {
    ZookeeperClient localClient = clientManager().getLocalClient();
    return new ZookeeperGroupRepository(
        localClient.getCuratorFramework(), objectMapper, zookeeperPaths());
  }

  @Bean
  CredentialsRepository credentialsRepository() {
    ZookeeperClient localClient = clientManager().getLocalClient();
    return new ZookeeperCredentialsRepository(
        localClient.getCuratorFramework(), objectMapper, zookeeperPaths());
  }

  @Bean
  TopicRepository topicRepository() {
    ZookeeperClient localClient = clientManager().getLocalClient();
    return new ZookeeperTopicRepository(
        localClient.getCuratorFramework(), objectMapper, zookeeperPaths(), groupRepository());
  }

  @Bean
  SubscriptionRepository subscriptionRepository() {
    ZookeeperClient localClient = clientManager().getLocalClient();
    return new ZookeeperSubscriptionRepository(
        localClient.getCuratorFramework(), objectMapper, zookeeperPaths(), topicRepository());
  }

  @Bean
  OAuthProviderRepository oAuthProviderRepository() {
    ZookeeperClient localClient = clientManager().getLocalClient();
    return new ZookeeperOAuthProviderRepository(
        localClient.getCuratorFramework(), objectMapper, zookeeperPaths());
  }

  @Bean
  MessagePreviewRepository messagePreviewRepository() {
    ZookeeperClient localClient = clientManager().getLocalClient();
    return new ZookeeperMessagePreviewRepository(
        localClient.getCuratorFramework(), objectMapper, zookeeperPaths());
  }

  @Bean
  WorkloadConstraintsRepository workloadConstraintsRepository() {
    ZookeeperClient localClient = clientManager().getLocalClient();
    return new ZookeeperWorkloadConstraintsRepository(
        localClient.getCuratorFramework(), objectMapper, zookeeperPaths());
  }

  @Bean
  @Primary
  @Qualifier("zookeeperOfflineRetransmissionRepository")
  OfflineRetransmissionRepository zookeeperOfflineRetransmissionRepository() {
    ZookeeperClient localClient = clientManager().getLocalClient();
    return new ZookeeperOfflineRetransmissionRepository(
        localClient.getCuratorFramework(), objectMapper, zookeeperPaths());
  }

  @Bean
  DatacenterReadinessRepository readinessRepository() {
    ZookeeperClient localClient = clientManager().getLocalClient();
    return new ZookeeperDatacenterReadinessRepository(
        localClient.getCuratorFramework(), objectMapper, zookeeperPaths());
  }

  @Bean
  InactiveTopicsRepository inactiveTopicsRepository() {
    ZookeeperClient localClient = clientManager().getLocalClient();
    return new ZookeeperInactiveTopicsRepository(
        localClient.getCuratorFramework(), objectMapper, zookeeperPaths());
  }

  @Bean(initMethod = "start", destroyMethod = "stop")
  public ModelAwareZookeeperNotifyingCache modelAwareZookeeperNotifyingCache(
      MetricsFacade metricsFacade,
      StorageClustersProperties zookeeperClustersProperties,
      DatacenterNameProvider datacenterNameProvider) {
    ZookeeperClient localClient = clientManager().getLocalClient();
    StorageProperties zookeeperProperties =
        zookeeperClustersProperties.toZookeeperProperties(datacenterNameProvider);
    return new ModelAwareZookeeperNotifyingCacheFactory(localClient.getCuratorFramework(), metricsFacade, zookeeperProperties)
        .provide();
  }

  @Bean
  public InternalNotificationsBus zookeeperInternalNotificationBus(
      ObjectMapper objectMapper, ModelAwareZookeeperNotifyingCache modelNotifyingCache) {
    return new ZookeeperInternalNotificationBus(objectMapper, modelNotifyingCache);
  }
}
