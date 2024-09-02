package pl.allegro.tech.hermes.consumers.config;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Clock;
import java.util.concurrent.ThreadFactory;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistryPaths;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

@Configuration
@EnableConfigurationProperties({KafkaClustersProperties.class, WorkloadProperties.class})
public class RegistryConfiguration {

  @Bean(initMethod = "start", destroyMethod = "stop")
  public ConsumerNodesRegistry consumerNodesRegistry(
      CuratorFramework curatorFramework,
      KafkaClustersProperties kafkaClustersProperties,
      WorkloadProperties workloadProperties,
      ZookeeperPaths zookeeperPaths,
      Clock clock,
      DatacenterNameProvider datacenterNameProvider) {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("ConsumerRegistryExecutor-%d").build();

    String consumerNodeId = workloadProperties.getNodeId();
    KafkaProperties kafkaProperties =
        kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);
    long deadAfterSeconds = workloadProperties.getDeadAfter().toSeconds();
    ConsumerNodesRegistryPaths registryPaths =
        new ConsumerNodesRegistryPaths(zookeeperPaths, kafkaProperties.getClusterName());

    return new ConsumerNodesRegistry(
        curatorFramework,
        newSingleThreadExecutor(threadFactory),
        registryPaths,
        consumerNodeId,
        deadAfterSeconds,
        clock);
  }
}
