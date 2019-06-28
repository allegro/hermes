package pl.allegro.tech.hermes.management.config.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminTool;
import pl.allegro.tech.hermes.common.config.Configs;
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
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;
import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistRepository;
import pl.allegro.tech.hermes.management.domain.dc.MultiDcRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.infrastructure.blacklist.ZookeeperTopicBlacklistRepository;
import pl.allegro.tech.hermes.management.infrastructure.dc.DcNameProvider;
import pl.allegro.tech.hermes.management.infrastructure.dc.DcNameSource;
import pl.allegro.tech.hermes.management.infrastructure.dc.DefaultDcNameProvider;
import pl.allegro.tech.hermes.management.infrastructure.dc.EnvironmentVariableDcNameProvider;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager;
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperRepositoryManager;

import javax.annotation.PostConstruct;

@Configuration
@EnableConfigurationProperties(StorageClustersProperties.class)
public class StorageConfiguration {

    @Autowired
    StorageClustersProperties storageClustersProperties;

    @Autowired
    ObjectMapper objectMapper;

    @Bean
    DcNameProvider dcNameProvider() {
        if(storageClustersProperties.getDcNameSource() == DcNameSource.ENV) {
            return new EnvironmentVariableDcNameProvider(storageClustersProperties.getDcNameSourceEnv());
        } else {
            return new DefaultDcNameProvider();
        }
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    ZookeeperClientManager clientManager() {
        return new ZookeeperClientManager(storageClustersProperties, dcNameProvider());
    }

    @Bean(initMethod = "start")
    ZookeeperRepositoryManager repositoryManager() {
        return new ZookeeperRepositoryManager(clientManager(), dcNameProvider(), objectMapper,
                zookeeperPaths());
    }

    @Bean
    ZookeeperPaths zookeeperPaths() {
        return new ZookeeperPaths(storageClustersProperties.getPathPrefix());
    }

    @Bean
    MultiDcRepositoryCommandExecutor multiDcRepositoryCommandExecutor() {
        return new MultiDcRepositoryCommandExecutor(repositoryManager(), storageClustersProperties.isTransactional());
    }

    @Bean
    public SharedCounter sharedCounter() {
        CuratorFramework curatorFramework = clientManager().getLocalClient().getCuratorFramework();
        return new SharedCounter(curatorFramework,
                storageClustersProperties.getSharedCountersExpiration(),
                storageClustersProperties.getRetrySleep(),
                storageClustersProperties.getRetryTimes());
    }

    @Bean
    public DistributedEphemeralCounter distributedCounter() {
        CuratorFramework curatorFramework = clientManager().getLocalClient().getCuratorFramework();
        return new DistributedEphemeralCounter(curatorFramework);
    }

    @Bean
    GroupRepository groupRepository() {
        ZookeeperClient localClient = clientManager().getLocalClient();
        return new ZookeeperGroupRepository(localClient.getCuratorFramework(), objectMapper, zookeeperPaths());
    }

    @Bean
    TopicRepository topicRepository() {
        ZookeeperClient localClient = clientManager().getLocalClient();
        return new ZookeeperTopicRepository(localClient.getCuratorFramework(), objectMapper, zookeeperPaths(),
                groupRepository());
    }

    @Bean
    SubscriptionRepository subscriptionRepository() {
        ZookeeperClient localClient = clientManager().getLocalClient();
        return new ZookeeperSubscriptionRepository(localClient.getCuratorFramework(), objectMapper, zookeeperPaths(),
                topicRepository());
    }

    @Bean
    OAuthProviderRepository oAuthProviderRepository() {
        ZookeeperClient localClient = clientManager().getLocalClient();
        return new ZookeeperOAuthProviderRepository(localClient.getCuratorFramework(), objectMapper, zookeeperPaths());
    }

    @Bean
    MessagePreviewRepository messagePreviewRepository() {
        ZookeeperClient localClient = clientManager().getLocalClient();
        return new ZookeeperMessagePreviewRepository(localClient.getCuratorFramework(), objectMapper, zookeeperPaths());
    }

    @Bean
    TopicBlacklistRepository topicBlacklistRepository() {
        ZookeeperClient localClient = clientManager().getLocalClient();
        return new ZookeeperTopicBlacklistRepository(localClient.getCuratorFramework(), objectMapper, zookeeperPaths());
    }

    @Bean
    SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator() {
        ZookeeperClient localClient = clientManager().getLocalClient();
        return new ZookeeperSubscriptionOffsetChangeIndicator(localClient.getCuratorFramework(), zookeeperPaths(),
                subscriptionRepository());
    }

    @Bean
    AdminTool adminTool() {
        ZookeeperClient localClient = clientManager().getLocalClient();
        return new ZookeeperAdminTool(zookeeperPaths(), localClient.getCuratorFramework(),
                objectMapper, Configs.ADMIN_REAPER_INTERAL_MS.getDefaultValue());
    }

    @PostConstruct
    public void init() {
        ensureInitPathExists();
        adminTool().start();
    }

    private void ensureInitPathExists() {
        ZookeeperClientManager clientManager = clientManager();
        for (ZookeeperClient client : clientManager.getClients()) {
            client.ensurePathExists(zookeeperPaths().groupsPath());
        }
    }
}
