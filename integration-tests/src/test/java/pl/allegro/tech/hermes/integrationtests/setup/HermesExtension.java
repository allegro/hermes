package pl.allegro.tech.hermes.integrationtests.setup;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;

import java.util.stream.Stream;

public class HermesExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static final ZookeeperContainer hermesZookeeper = new ZookeeperContainer("HermesZookeeper");
    private static final KafkaContainerCluster kafka = new KafkaContainerCluster(1);
    private static final HermesConsumersTestApp consumers = new HermesConsumersTestApp(hermesZookeeper, kafka);
    private static final HermesManagementTestApp management = new HermesManagementTestApp(hermesZookeeper, kafka);
    private static final HermesFrontendTestApp frontend = new HermesFrontendTestApp(hermesZookeeper, kafka);

    private static boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!started) {
            Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::start);
            management.start();
            Stream.of(consumers, frontend).parallel().forEach(HermesTestApp::start);
            started = true;
        }
    }

    @Override
    public void close() {
        Stream.of(management, consumers, frontend).parallel().forEach(HermesTestApp::stop);
        Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::stop);
        started = false;
    }

    public String getManagementUrl() {
        return "http://localhost:" + management.getPort();
    }

    public String getFrontendUrl() {
        return "http://localhost:" + frontend.getPort();
    }
}
