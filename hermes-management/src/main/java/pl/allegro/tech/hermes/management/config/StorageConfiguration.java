package pl.allegro.tech.hermes.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperProperties;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc.DcNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc.DefaultDcNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.commands.ZookeeperCommandFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandExecutor;
import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistRepository;
import pl.allegro.tech.hermes.management.infrastructure.blacklist.DistributedZookeeperTopicBlacklistRepository;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(StorageClustersProperties.class)
public class StorageConfiguration {

    @Autowired
    StorageClustersProperties zookeeperClustersProperties;

    @Autowired
    ObjectMapper objectMapper;

    @Bean
    DcNameProvider dcNameProvider() {
        return new DefaultDcNameProvider();
    }

    @Bean
    @ConfigurationProperties(prefix = "zookeeper")
    ZookeeperProperties zookeeperProperties() {
        return new ZookeeperProperties();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    ZookeeperClientManager clientManager() {
        return new ZookeeperClientManager(zookeeperProperties(), dcNameProvider());
    }

    @Bean
    CuratorFramework curatorFramework() {
        return clientManager().getLocalClient().getCuratorFramework();
    }

    @Bean
    ZookeeperPaths zookeeperPaths() {
        return new ZookeeperPaths(zookeeperClustersProperties.getPathPrefix());
    }

    @Bean
    public SharedCounter sharedCounter() {
        ZookeeperProperties properties = zookeeperProperties();
        return new SharedCounter(curatorFramework(),
                properties.getSharedCountersExpiration(),
                properties.getRetrySleep(),
                properties.getRetryTimes());
    }

    @Bean
    public DistributedEphemeralCounter distributedCounter() {
        return new DistributedEphemeralCounter(curatorFramework());
    }

    @Bean
    ExecutorService zookeeperCommExecutorService() {
        return Executors.newFixedThreadPool(zookeeperProperties().getCommandPoolSize());
    }

    @Bean
    ZookeeperCommandExecutor commandExecutor() {
        ZookeeperProperties properties = zookeeperProperties();

        return new ZookeeperCommandExecutor(
                clientManager(),
                properties.getCommandPoolSize(),
                properties.isTransactional()
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
        return new DistributedZookeeperMessagePreviewRepository(clientManager(), commandExecutor(),
                zookeeperPaths(), objectMapper);
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
        return new ZookeeperAdminTool(zookeeperPaths(), curatorFramework(),
                objectMapper, Configs.ADMIN_REAPER_INTERAL_MS.getDefaultValue());
    }

    @Bean
    UndeliveredMessageLog undeliveredMessageLog() {
        return new DistributedZookeeperUndeliveredMessageLog(clientManager(), commandExecutor(), zookeeperPaths(),
                zookeeperCommExecutorService(), objectMapper);
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
