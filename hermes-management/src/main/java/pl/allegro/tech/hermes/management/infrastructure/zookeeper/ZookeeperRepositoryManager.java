package pl.allegro.tech.hermes.management.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminTool;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.undelivered.LastUndeliveredMessageReader;
import pl.allegro.tech.hermes.common.message.undelivered.ZookeeperLastUndeliveredMessageReader;
import pl.allegro.tech.hermes.domain.CredentialsRepository;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperCredentialsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperMessagePreviewRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperOAuthProviderRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperWorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.config.storage.ZookeeperGroupRepositoryFactory;
import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsRepository;
import pl.allegro.tech.hermes.management.domain.readiness.DatacenterReadinessRepository;
import pl.allegro.tech.hermes.management.domain.retransmit.OfflineRetransmissionRepository;
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupToDeleteRepository;
import pl.allegro.tech.hermes.management.infrastructure.blacklist.ZookeeperTopicBlacklistRepository;
import pl.allegro.tech.hermes.management.infrastructure.detection.ZookeeperInactiveTopicsRepository;
import pl.allegro.tech.hermes.management.infrastructure.readiness.ZookeeperDatacenterReadinessRepository;
import pl.allegro.tech.hermes.management.infrastructure.retransmit.ZookeeperOfflineRetransmissionRepository;
import pl.allegro.tech.hermes.management.infrastructure.subscription.consumergroup.ZookeeperConsumerGroupToDeleteRepository;

public class ZookeeperRepositoryManager implements RepositoryManager {

  private final DatacenterNameProvider datacenterNameProvider;
  private final ObjectMapper mapper;
  private final ZookeeperPaths paths;
  private final ZookeeperClientManager clientManager;
  private final Map<Class<?>, Object> repositoryByType = new HashMap<>();
  private final Map<String, GroupRepository> groupRepositoriesByDc = new HashMap<>();
  private final Map<String, CredentialsRepository> credentialsRepositoriesByDc = new HashMap<>();
  private final Map<String, TopicRepository> topicRepositoriesByDc = new HashMap<>();
  private final Map<String, SubscriptionRepository> subscriptionRepositoriesByDc = new HashMap<>();
  private final Map<String, OAuthProviderRepository> oAuthProviderRepositoriesByDc =
      new HashMap<>();
  private final Map<String, SubscriptionOffsetChangeIndicator> offsetChangeIndicatorsByDc =
      new HashMap<>();
  private final Map<String, MessagePreviewRepository> messagePreviewRepositoriesByDc =
      new HashMap<>();
  private final Map<String, TopicBlacklistRepository> topicBlacklistRepositoriesByDc =
      new HashMap<>();
  private final Map<String, WorkloadConstraintsRepository> workloadConstraintsRepositoriesByDc =
      new HashMap<>();
  private final Map<String, LastUndeliveredMessageReader> lastUndeliveredMessageReaderByDc =
      new HashMap<>();
  private final Map<String, AdminTool> adminToolByDc = new HashMap<>();
  private final Map<String, DatacenterReadinessRepository> readinessRepositoriesByDc =
      new HashMap<>();
  private final Map<String, OfflineRetransmissionRepository> offlineRetransmissionRepositoriesByDc =
      new HashMap<>();
  private final Map<String, InactiveTopicsRepository> inactiveTopicsRepositoriesByDc =
      new HashMap<>();
  private final Map<String, ConsumerGroupToDeleteRepository> consumerGroupCleanupRepositoryByDc =
      new HashMap<>();
  private final ZookeeperGroupRepositoryFactory zookeeperGroupRepositoryFactory;

  public ZookeeperRepositoryManager(
      ZookeeperClientManager clientManager,
      DatacenterNameProvider datacenterNameProvider,
      ObjectMapper mapper,
      ZookeeperPaths paths,
      ZookeeperGroupRepositoryFactory zookeeperGroupRepositoryFactory) {
    this.datacenterNameProvider = datacenterNameProvider;
    this.mapper = mapper;
    this.paths = paths;
    this.clientManager = clientManager;
    this.zookeeperGroupRepositoryFactory = zookeeperGroupRepositoryFactory;
    initRepositoryTypeMap();
  }

  public void start() {
    for (ZookeeperClient client : clientManager.getClients()) {
      String dcName = client.getDatacenterName();
      CuratorFramework zookeeper = client.getCuratorFramework();

      GroupRepository groupRepository =
          zookeeperGroupRepositoryFactory.create(zookeeper, mapper, paths);
      groupRepositoriesByDc.put(dcName, groupRepository);

      CredentialsRepository credentialsRepository =
          new ZookeeperCredentialsRepository(zookeeper, mapper, paths);
      credentialsRepositoriesByDc.put(dcName, credentialsRepository);

      TopicRepository topicRepository =
          new ZookeeperTopicRepository(zookeeper, mapper, paths, groupRepository);
      topicRepositoriesByDc.put(dcName, topicRepository);

      SubscriptionRepository subscriptionRepository =
          new ZookeeperSubscriptionRepository(zookeeper, mapper, paths, topicRepository);
      subscriptionRepositoriesByDc.put(dcName, subscriptionRepository);

      OAuthProviderRepository oAuthProviderRepository =
          new ZookeeperOAuthProviderRepository(zookeeper, mapper, paths);
      oAuthProviderRepositoriesByDc.put(dcName, oAuthProviderRepository);

      SubscriptionOffsetChangeIndicator offsetChangeIndicator =
          new ZookeeperSubscriptionOffsetChangeIndicator(zookeeper, paths, subscriptionRepository);
      offsetChangeIndicatorsByDc.put(dcName, offsetChangeIndicator);

      MessagePreviewRepository messagePreviewRepository =
          new ZookeeperMessagePreviewRepository(zookeeper, mapper, paths);
      messagePreviewRepositoriesByDc.put(dcName, messagePreviewRepository);

      TopicBlacklistRepository topicBlacklistRepository =
          new ZookeeperTopicBlacklistRepository(zookeeper, mapper, paths);
      topicBlacklistRepositoriesByDc.put(dcName, topicBlacklistRepository);

      WorkloadConstraintsRepository workloadConstraintsRepository =
          new ZookeeperWorkloadConstraintsRepository(zookeeper, mapper, paths);
      workloadConstraintsRepositoriesByDc.put(dcName, workloadConstraintsRepository);

      LastUndeliveredMessageReader lastUndeliveredMessageReader =
          new ZookeeperLastUndeliveredMessageReader(zookeeper, paths, mapper);
      lastUndeliveredMessageReaderByDc.put(dcName, lastUndeliveredMessageReader);

      AdminTool adminTool = new ZookeeperAdminTool(paths, client.getCuratorFramework(), mapper);
      adminToolByDc.put(dcName, adminTool);

      DatacenterReadinessRepository readinessRepository =
          new ZookeeperDatacenterReadinessRepository(zookeeper, mapper, paths);
      readinessRepositoriesByDc.put(dcName, readinessRepository);

      ZookeeperOfflineRetransmissionRepository offlineRetransmissionRepository =
          new ZookeeperOfflineRetransmissionRepository(zookeeper, mapper, paths);
      offlineRetransmissionRepositoriesByDc.put(dcName, offlineRetransmissionRepository);

      ZookeeperInactiveTopicsRepository inactiveTopicsRepository =
          new ZookeeperInactiveTopicsRepository(zookeeper, mapper, paths);
      inactiveTopicsRepositoriesByDc.put(dcName, inactiveTopicsRepository);

      ZookeeperConsumerGroupToDeleteRepository consumerGroupCleanupRepository =
          new ZookeeperConsumerGroupToDeleteRepository(zookeeper, mapper, paths);
      consumerGroupCleanupRepositoryByDc.put(dcName, consumerGroupCleanupRepository);
    }
  }

  public <T> DatacenterBoundRepositoryHolder<T> getLocalRepository(Class<T> repositoryType) {
    String dcName = datacenterNameProvider.getDatacenterName();
    T repository = getRepositoriesByType(repositoryType).get(dcName);

    if (repository == null) {
      throw new InternalProcessingException(
          "Failed to find '"
              + repositoryType.getSimpleName()
              + "' bound with DC '"
              + dcName
              + "'.");
    }

    return new DatacenterBoundRepositoryHolder<>(repository, dcName);
  }

  public <T> List<DatacenterBoundRepositoryHolder<T>> getRepositories(Class<T> repositoryType) {
    return getRepositoriesByType(repositoryType).entrySet().stream()
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .map(entry -> new DatacenterBoundRepositoryHolder<>(entry.getValue(), entry.getKey()))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  public <T> Map<String, T> getRepositoriesByType(Class<T> type) {
    Object repository = repositoryByType.get(type);
    if (repository == null) {
      throw new InternalProcessingException(
          "Could not provide repository of type: " + type.getName());
    }
    return (Map<String, T>) repository;
  }

  private void initRepositoryTypeMap() {
    repositoryByType.put(GroupRepository.class, groupRepositoriesByDc);
    repositoryByType.put(CredentialsRepository.class, credentialsRepositoriesByDc);
    repositoryByType.put(TopicRepository.class, topicRepositoriesByDc);
    repositoryByType.put(SubscriptionRepository.class, subscriptionRepositoriesByDc);
    repositoryByType.put(OAuthProviderRepository.class, oAuthProviderRepositoriesByDc);
    repositoryByType.put(SubscriptionOffsetChangeIndicator.class, offsetChangeIndicatorsByDc);
    repositoryByType.put(MessagePreviewRepository.class, messagePreviewRepositoriesByDc);
    repositoryByType.put(TopicBlacklistRepository.class, topicBlacklistRepositoriesByDc);
    repositoryByType.put(WorkloadConstraintsRepository.class, workloadConstraintsRepositoriesByDc);
    repositoryByType.put(LastUndeliveredMessageReader.class, lastUndeliveredMessageReaderByDc);
    repositoryByType.put(AdminTool.class, adminToolByDc);
    repositoryByType.put(DatacenterReadinessRepository.class, readinessRepositoriesByDc);
    repositoryByType.put(
        OfflineRetransmissionRepository.class, offlineRetransmissionRepositoriesByDc);
    repositoryByType.put(InactiveTopicsRepository.class, inactiveTopicsRepositoriesByDc);
    repositoryByType.put(ConsumerGroupToDeleteRepository.class, consumerGroupCleanupRepositoryByDc);
  }
}
