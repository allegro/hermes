package pl.allegro.tech.hermes.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.admin.AdminTool;
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminTool;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.message.undelivered.ZookeeperUndeliveredMessageLog;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.common.kafka.offset.SubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionOffsetChangeIndicator;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(StorageConfiguration.class);

    @Autowired
    StorageProperties storageProperties;

    @Autowired
    ObjectMapper objectMapper;

    @Bean(name = "storageZookeeper")
    CuratorFramework storageZookeeper() {
        CuratorFramework curator = CuratorFrameworkFactory.builder()
                .connectString(storageProperties.getConnectionString())
                .sessionTimeoutMs(storageProperties.getSessionTimeout())
                .connectionTimeoutMs(storageProperties.getConnectTimeout())
                .retryPolicy(new ExponentialBackoffRetry(storageProperties.getRetrySleep(), storageProperties.getRetryTimes()))
                .build();

        startAndWaitForConnection(curator);

        return curator;
    }

    private void startAndWaitForConnection(CuratorFramework curator) {
        curator.start();
        try {
            curator.blockUntilConnected();
        } catch (InterruptedException interruptedException) {
            RuntimeException exception = new InternalProcessingException("Could not start curator for storage", interruptedException);
            logger.error(exception.getMessage(), interruptedException);
            throw exception;
        }
    }

    @Bean
    ZookeeperPaths zookeeperPaths() {
        return new ZookeeperPaths(storageProperties.getPathPrefix());
    }

    @Bean
    public SharedCounter sharedCounter() {
        return new SharedCounter(storageZookeeper(),
                storageProperties.getSharedCountersExpiration(),
                storageProperties.getRetrySleep(),
                storageProperties.getRetryTimes());
    }

    @Bean
    public DistributedEphemeralCounter distributedCounter() {
        return new DistributedEphemeralCounter(storageZookeeper());
    }

    @Bean
    GroupRepository groupRepository() {
        return new ZookeeperGroupRepository(storageZookeeper(), objectMapper, zookeeperPaths());
    }

    @Bean
    TopicRepository topicRepository() {
        return new ZookeeperTopicRepository(storageZookeeper(), objectMapper, zookeeperPaths(), groupRepository());
    }

    @Bean
    SubscriptionRepository subscriptionRepository() {
        return new ZookeeperSubscriptionRepository(storageZookeeper(), objectMapper, zookeeperPaths(), topicRepository());
    }

    @Bean
    SubscriptionOffsetChangeIndicator subscriptionOffsetChangeIndicator() {
        return new ZookeeperSubscriptionOffsetChangeIndicator(storageZookeeper(), zookeeperPaths(), subscriptionRepository());
    }

    @Bean
    AdminTool adminTool() {
        return new ZookeeperAdminTool(zookeeperPaths(), storageZookeeper(), objectMapper,
                Configs.ADMIN_REAPER_INTERAL_MS.getDefaultValue());
    }

    @Bean
    UndeliveredMessageLog undeliveredMessageLog() {
        return new ZookeeperUndeliveredMessageLog(storageZookeeper(), zookeeperPaths(), objectMapper);
    }
}
