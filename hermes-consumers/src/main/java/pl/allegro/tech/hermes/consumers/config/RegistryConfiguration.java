package pl.allegro.tech.hermes.consumers.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistryPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import java.time.Clock;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

@Configuration
@EnableConfigurationProperties({
        KafkaProperties.class,
        WorkloadProperties.class
})
public class RegistryConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public ConsumerNodesRegistry consumerNodesRegistry(CuratorFramework curatorFramework,
                                                       KafkaProperties kafkaProperties,
                                                       WorkloadProperties workloadProperties,
                                                       ZookeeperPaths zookeeperPaths,
                                                       Clock clock) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("ConsumerRegistryExecutor-%d").build();

        String consumerNodeId = workloadProperties.getNodeId();
        long deadAfterSeconds = workloadProperties.getDeadAfter().toSeconds();
        ConsumerNodesRegistryPaths registryPaths = new ConsumerNodesRegistryPaths(zookeeperPaths, kafkaProperties.getClusterName());

        return new ConsumerNodesRegistry(curatorFramework, newSingleThreadExecutor(threadFactory),
                registryPaths, consumerNodeId, deadAfterSeconds, clock);
    }
}
