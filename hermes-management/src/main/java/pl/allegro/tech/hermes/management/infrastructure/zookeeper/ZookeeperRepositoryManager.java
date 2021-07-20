package pl.allegro.tech.hermes.management.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminTool;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.message.undelivered.ZookeeperUndeliveredMessageLog;
import pl.allegro.tech.hermes.domain.CredentialsRepository;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperCredentialsRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperMessagePreviewRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperOAuthProviderRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperDatacenterReadinessRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperWorkloadConstraintsRepository;
import pl.allegro.tech.hermes.management.config.storage.ZookeeperGroupRepositoryFactory;
import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistRepository;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager;
import pl.allegro.tech.hermes.management.infrastructure.blacklist.ZookeeperTopicBlacklistRepository;
import pl.allegro.tech.hermes.management.infrastructure.dc.DatacenterNameProvider;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZookeeperRepositoryManager implements RepositoryManager {

    private final DatacenterNameProvider datacenterNameProvider;
    private final ObjectMapper mapper;
    private final ZookeeperPaths paths;
    private final ZookeeperClientManager clientManager;
    private ZookeeperGroupRepositoryFactory zookeeperGroupRepositoryFactory;
    private Integer adminReaperInterval;

    private final Map<Class<?>, Object> repositoryByType = new HashMap<>();

    private final Map<String, GroupRepository> groupRepositoriesByDc = new HashMap<>();
    private final Map<String, CredentialsRepository> credentialsRepositoriesByDc = new HashMap<>();
    private final Map<String, TopicRepository> topicRepositoriesByDc = new HashMap<>();
    private final Map<String, SubscriptionRepository> subscriptionRepositoriesByDc = new HashMap<>();
    private final Map<String, OAuthProviderRepository> oAuthProviderRepositoriesByDc = new HashMap<>();
    private final Map<String, SubscriptionOffsetChangeIndicator> offsetChangeIndicatorsByDc = new HashMap<>();
    private final Map<String, MessagePreviewRepository> messagePreviewRepositoriesByDc = new HashMap<>();
    private final Map<String, TopicBlacklistRepository> topicBlacklistRepositoriesByDc = new HashMap<>();
    private final Map<String, WorkloadConstraintsRepository> workloadConstraintsRepositoriesByDc = new HashMap<>();
    private final Map<String, UndeliveredMessageLog> undeliveredMessageLogsByDc = new HashMap<>();
    private final Map<String, AdminTool> adminToolByDc = new HashMap<>();
    private final Map<String, ReadinessRepository> readinessRepositoriesByDc = new HashMap<>();

    public ZookeeperRepositoryManager(ZookeeperClientManager clientManager,
                                      DatacenterNameProvider datacenterNameProvider,
                                      ObjectMapper mapper,
                                      ZookeeperPaths paths,
                                      ZookeeperGroupRepositoryFactory zookeeperGroupRepositoryFactory,
                                      Integer adminReaperInterval) {
        this.datacenterNameProvider = datacenterNameProvider;
        this.mapper = mapper;
        this.paths = paths;
        this.clientManager = clientManager;
        this.zookeeperGroupRepositoryFactory = zookeeperGroupRepositoryFactory;
        this.adminReaperInterval = adminReaperInterval;
        initRepositoryTypeMap();
    }

    public void start() {
        for (ZookeeperClient client : clientManager.getClients()) {
            String dcName = client.getDatacenterName();
            CuratorFramework zookeeper = client.getCuratorFramework();

            GroupRepository groupRepository = zookeeperGroupRepositoryFactory.create(zookeeper, mapper, paths);
            CredentialsRepository credentialsRepository = new ZookeeperCredentialsRepository(zookeeper, mapper, paths);
            TopicRepository topicRepository = new ZookeeperTopicRepository(zookeeper, mapper, paths, groupRepository);
            SubscriptionRepository subscriptionRepository = new ZookeeperSubscriptionRepository(zookeeper, mapper,
                    paths, topicRepository);
            OAuthProviderRepository oAuthProviderRepository = new ZookeeperOAuthProviderRepository(zookeeper, mapper,
                    paths);
            SubscriptionOffsetChangeIndicator offsetChangeIndicator =
                    new ZookeeperSubscriptionOffsetChangeIndicator(zookeeper, paths, subscriptionRepository);
            MessagePreviewRepository messagePreviewRepository = new ZookeeperMessagePreviewRepository(zookeeper, mapper,
                    paths);
            TopicBlacklistRepository topicBlacklistRepository = new ZookeeperTopicBlacklistRepository(zookeeper, mapper,
                    paths);
            WorkloadConstraintsRepository workloadConstraintsRepository = new ZookeeperWorkloadConstraintsRepository(
                    zookeeper, mapper, paths);
            UndeliveredMessageLog undeliveredMessageLog = new ZookeeperUndeliveredMessageLog(zookeeper, paths, mapper);
            AdminTool adminTool = new ZookeeperAdminTool(paths, client.getCuratorFramework(),
                    mapper, adminReaperInterval);

            ReadinessRepository readinessRepository = new ZookeeperDatacenterReadinessRepository(zookeeper, mapper, paths);
            adminTool.start();

            groupRepositoriesByDc.put(dcName, groupRepository);
            credentialsRepositoriesByDc.put(dcName, credentialsRepository);
            topicRepositoriesByDc.put(dcName, topicRepository);
            subscriptionRepositoriesByDc.put(dcName, subscriptionRepository);
            oAuthProviderRepositoriesByDc.put(dcName, oAuthProviderRepository);
            offsetChangeIndicatorsByDc.put(dcName, offsetChangeIndicator);
            messagePreviewRepositoriesByDc.put(dcName, messagePreviewRepository);
            topicBlacklistRepositoriesByDc.put(dcName, topicBlacklistRepository);
            workloadConstraintsRepositoriesByDc.put(dcName, workloadConstraintsRepository);
            undeliveredMessageLogsByDc.put(dcName, undeliveredMessageLog);
            adminToolByDc.put(dcName, adminTool);
            readinessRepositoriesByDc.put(dcName, readinessRepository);
        }
    }

    public <T> DatacenterBoundRepositoryHolder<T> getLocalRepository(Class<T> repositoryType) {
        String dcName = datacenterNameProvider.getDatacenterName();
        T repository = getRepositoriesByType(repositoryType).get(dcName);

        if (repository == null) {
            throw new InternalProcessingException("Failed to find '" + repositoryType.getSimpleName() +
                    "' bound with DC '" + dcName + "'.");
        }

        return new DatacenterBoundRepositoryHolder<>(repository, dcName);
    }

    public <T> List<DatacenterBoundRepositoryHolder<T>> getRepositories(Class<T> repositoryType) {
        return getRepositoriesByType(repositoryType)
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> new DatacenterBoundRepositoryHolder<>(entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, T> getRepositoriesByType(Class<T> type) {
        Object repository = repositoryByType.get(type);
        if (repository == null) {
            throw new InternalProcessingException("Could not provide repository of type: " + type.getName());
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
        repositoryByType.put(UndeliveredMessageLog.class, undeliveredMessageLogsByDc);
        repositoryByType.put(AdminTool.class, adminToolByDc);
        repositoryByType.put(ReadinessRepository.class, readinessRepositoriesByDc);
    }
}
