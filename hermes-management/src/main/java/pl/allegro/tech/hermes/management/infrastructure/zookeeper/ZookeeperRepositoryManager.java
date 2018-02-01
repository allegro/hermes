package pl.allegro.tech.hermes.management.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.message.undelivered.ZookeeperUndeliveredMessageLog;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperMessagePreviewRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperOAuthProviderRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;
import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistRepository;
import pl.allegro.tech.hermes.management.domain.dc.DcBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager;
import pl.allegro.tech.hermes.management.infrastructure.blacklist.ZookeeperTopicBlacklistRepository;
import pl.allegro.tech.hermes.management.infrastructure.dc.DcNameProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZookeeperRepositoryManager implements RepositoryManager {

    private final DcNameProvider dcNameProvider;
    private final ObjectMapper mapper;
    private final ZookeeperPaths paths;
    private final ZookeeperClientManager clientManager;

    private final Map<String, GroupRepository> groupRepositoriesByDc = new HashMap<>();
    private final Map<String, TopicRepository> topicRepositoriesByDc = new HashMap<>();
    private final Map<String, SubscriptionRepository> subscriptionRepositoriesByDc = new HashMap<>();
    private final Map<String, OAuthProviderRepository> oAuthProviderRepositoriesByDc = new HashMap<>();
    private final Map<String, SubscriptionOffsetChangeIndicator> offsetChangeIndicatorsByDc = new HashMap<>();
    private final Map<String, MessagePreviewRepository> messagePreviewRepositoriesByDc = new HashMap<>();
    private final Map<String, TopicBlacklistRepository> topicBlacklistRepositoriesByDc = new HashMap<>();
    private final Map<String, UndeliveredMessageLog> undeliveredMessageLogsByDc = new HashMap<>();

    public ZookeeperRepositoryManager(ZookeeperClientManager clientManager, DcNameProvider dcNameProvider,
                                      ObjectMapper mapper, ZookeeperPaths paths) {
        this.dcNameProvider = dcNameProvider;
        this.mapper = mapper;
        this.paths = paths;
        this.clientManager = clientManager;
    }

    public void start() {
        for (ZookeeperClient client : clientManager.getClients()) {
            String dcName = client.getDcName();
            CuratorFramework zookeeper = client.getCuratorFramework();

            GroupRepository groupRepository = new ZookeeperGroupRepository(zookeeper, mapper, paths);
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
            UndeliveredMessageLog undeliveredMessageLog = new ZookeeperUndeliveredMessageLog(zookeeper, paths, mapper);

            groupRepositoriesByDc.put(dcName, groupRepository);
            topicRepositoriesByDc.put(dcName, topicRepository);
            subscriptionRepositoriesByDc.put(dcName, subscriptionRepository);
            oAuthProviderRepositoriesByDc.put(dcName, oAuthProviderRepository);
            offsetChangeIndicatorsByDc.put(dcName, offsetChangeIndicator);
            messagePreviewRepositoriesByDc.put(dcName, messagePreviewRepository);
            topicBlacklistRepositoriesByDc.put(dcName, topicBlacklistRepository);
            undeliveredMessageLogsByDc.put(dcName, undeliveredMessageLog);
        }
    }

    public <T> DcBoundRepositoryHolder<T> getLocalRepository(Class<T> repositoryType) {
        String dcName = dcNameProvider.getDcName();
        T repository = getRepositoriesByType(repositoryType).get(dcName);

        if (repository == null) {
            throw new InternalProcessingException("Failed to find '" + repositoryType.getSimpleName() +
                    "' bound with DC '" + dcName + "'.");
        }

        return new DcBoundRepositoryHolder<>(repository, dcName);
    }

    public <T> List<DcBoundRepositoryHolder<T>> getRepositories(Class<T> repositoryType) {
        return getRepositoriesByType(repositoryType)
                .entrySet()
                .stream()
                .map(entry -> new DcBoundRepositoryHolder<>(entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, T> getRepositoriesByType(Class<T> type) {
        if (type == GroupRepository.class) {
            return (Map<String, T>) groupRepositoriesByDc;
        } else if (type == TopicRepository.class) {
            return (Map<String, T>) topicRepositoriesByDc;
        } else if (type == SubscriptionRepository.class) {
            return (Map<String, T>) subscriptionRepositoriesByDc;
        } else if (type == OAuthProviderRepository.class) {
            return (Map<String, T>) oAuthProviderRepositoriesByDc;
        } else if (type == SubscriptionOffsetChangeIndicator.class) {
            return (Map<String, T>) offsetChangeIndicatorsByDc;
        } else if (type == MessagePreviewRepository.class) {
            return (Map<String, T>) messagePreviewRepositoriesByDc;
        } else if (type == TopicBlacklistRepository.class) {
            return (Map<String, T>) topicBlacklistRepositoriesByDc;
        } else if (type == UndeliveredMessageLog.class) {
            return (Map<String, T>) undeliveredMessageLogsByDc;
        } else {
            throw new InternalProcessingException("Could not provide repository of type: " + type.getName());
        }
    }
}
