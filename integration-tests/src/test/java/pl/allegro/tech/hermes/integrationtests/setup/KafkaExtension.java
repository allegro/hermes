package pl.allegro.tech.hermes.integrationtests.setup;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;

public class KafkaExtension implements BeforeAllCallback {
    public final KafkaContainerCluster kafkaCluster = new KafkaContainerCluster(1);

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        kafkaCluster.start();
    }
}
