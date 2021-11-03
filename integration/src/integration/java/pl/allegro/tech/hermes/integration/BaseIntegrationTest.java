package pl.allegro.tech.hermes.integration;

import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;

import java.util.stream.Stream;

public class BaseIntegrationTest {
    private static final int NUMBER_OF_BROKERS_PER_CLUSTER = 3;

    static final KafkaContainerCluster kafkaClusterOne = new KafkaContainerCluster(NUMBER_OF_BROKERS_PER_CLUSTER);
    static final KafkaContainerCluster kafkaClusterTwo = new KafkaContainerCluster(NUMBER_OF_BROKERS_PER_CLUSTER);
    static final ZookeeperContainer hermesZookeeperOne = new ZookeeperContainer();
    static final ZookeeperContainer hermesZookeeperTwo = new ZookeeperContainer();

    static {
        Stream.of(kafkaClusterOne, kafkaClusterTwo, hermesZookeeperOne, hermesZookeeperTwo)
                .parallel()
                .forEach(Startable::start);
    }
}
