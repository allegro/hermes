package pl.allegro.tech.hermes.management.config.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.DistributedZookeeperGroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.DistributedZookeeperMessagePreviewRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.DistributedZookeeperOAuthProviderRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.DistributedZookeeperSubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.infrastructure.zookeeper.DistributedZookeeperSubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.DistributedZookeeperTopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.DistributedZookeeperUndeliveredMessageLog;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClusterProperties;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperProperties;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc.DcNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc.DcNameSource;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc.DefaultDcNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc.EnvironmentVariableDcNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.commands.ZookeeperCommandFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandExecutor;
import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistRepository;
import pl.allegro.tech.hermes.management.infrastructure.blacklist.DistributedZookeeperTopicBlacklistRepository;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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
        ZookeeperProperties properties = new ZookeeperPropertiesMapper()
                .fromStorageClustersProperies(storageClustersProperties);
        return new ZookeeperClientManager(properties, dcNameProvider());
    }

    @Bean
    ZookeeperPaths zookeeperPaths() {
        return new ZookeeperPaths(storageClustersProperties.getPathPrefix());
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
    ExecutorService zookeeperLinkExecutorService() {
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("zk-link-%d").build();
        return Executors.newFixedThreadPool(storageClustersProperties.getMaxConcurrentOperations(), factory);
    }

    @Bean
    ZookeeperCommandExecutor commandExecutor() {
        return new ZookeeperCommandExecutor(
                clientManager(),
                zookeeperLinkExecutorService(),
                storageClustersProperties.isTransactional()
        );
    }

    @Bean
    ZookeeperCommandFactory commandFactory() {
        return new ZookeeperCommandFactory(zookeeperPaths(), objectMapper);
    }

    @Bean
    GroupRepository groupRepository() {
        return new DistributedZookeeperGroupRepository(clientManager(), commandExecutor(), commandFactory(),
                zookeeperPaths(), objectMapper);
    }

    @Bean
    TopicRepository topicRepository() {
        return new DistributedZookeeperTopicRepository(clientManager(), commandExecutor(), commandFactory(),
                zookeeperPaths(), objectMapper);
    }

    @Bean
    SubscriptionRepository subscriptionRepository() {
        return new DistributedZookeeperSubscriptionRepository(clientManager(), commandExecutor(), commandFactory(),
                zookeeperPaths(), objectMapper);
    }

    @Bean
    OAuthProviderRepository oAuthProviderRepository() {
        return new DistributedZookeeperOAuthProviderRepository(clientManager(), commandExecutor(), commandFactory(),
                zookeeperPaths(), objectMapper);
    }

    @Bean
    MessagePreviewRepository messagePreviewRepository() {
        return new DistributedZookeeperMessagePreviewRepository(clientManager(), zookeeperPaths(), objectMapper);
    }

    @Bean
    TopicBlacklistRepository topicBlacklistRepository() {
        return new DistributedZookeeperTopicBlacklistRepository(clientManager(), commandExecutor(), zookeeperPaths(),
                objectMapper);
    }

    @Bean
    SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator() {
        return new DistributedZookeeperSubscriptionOffsetChangeIndicator(clientManager(), commandExecutor(),
                commandFactory(), zookeeperPaths(), objectMapper);
    }

    @Bean
    AdminTool adminTool() {
        CuratorFramework curatorFramework = clientManager().getLocalClient().getCuratorFramework();
        return new ZookeeperAdminTool(zookeeperPaths(), curatorFramework,
                objectMapper, Configs.ADMIN_REAPER_INTERAL_MS.getDefaultValue());
    }

    @Bean
    UndeliveredMessageLog undeliveredMessageLog() {
        return new DistributedZookeeperUndeliveredMessageLog(clientManager(), zookeeperPaths(),
                zookeeperLinkExecutorService(), objectMapper);
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
