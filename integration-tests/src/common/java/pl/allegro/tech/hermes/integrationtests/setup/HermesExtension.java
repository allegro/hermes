package pl.allegro.tech.hermes.integrationtests.setup;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.integrationtests.client.HermesTestClient;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscriber;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestSubscribersExtension;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;

import java.util.stream.Stream;

import static pl.allegro.tech.hermes.integrationtests.setup.HermesManagementTestApp.AUDIT_EVENT_PATH;

public class HermesExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    public static final TestSubscribersExtension auditEventsReceiver = new TestSubscribersExtension();

    private static final ZookeeperContainer hermesZookeeper = new ZookeeperContainer("HermesZookeeper");
    private static final KafkaContainerCluster kafka = new KafkaContainerCluster(1);
    public static final ConfluentSchemaRegistryContainer schemaRegistry = new ConfluentSchemaRegistryContainer()
            .withKafkaCluster(kafka);
    private static final HermesConsumersTestApp consumers = new HermesConsumersTestApp(hermesZookeeper, kafka, schemaRegistry);
    private static final HermesManagementTestApp management = new HermesManagementTestApp(hermesZookeeper, kafka, schemaRegistry);
    private static final HermesFrontendTestApp frontend = new HermesFrontendTestApp(hermesZookeeper, kafka, schemaRegistry);
    private HermesTestClient hermesTestClient;
    private HermesInitHelper hermesInitHelper;

    private static boolean started = false;

    public static TestSubscriber auditEvents;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!started) {
            Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::start);
            schemaRegistry.start();
            management.addEventAuditorListenerPort(auditEventsReceiver.getPort());
            management.start();
            Stream.of(consumers, frontend).forEach(HermesTestApp::start);
            started = true;
        }
        hermesTestClient = new HermesTestClient(management.getPort(), frontend.getPort(), consumers.getPort());
        hermesInitHelper = new HermesInitHelper(management.getPort());
        auditEvents = auditEventsReceiver.createSubscriberWithStrictPath(200, AUDIT_EVENT_PATH);
    }

    @Override
    public void close() {
        Stream.of(management, consumers, frontend).parallel().forEach(HermesTestApp::stop);
        Stream.of(hermesZookeeper, kafka, schemaRegistry).parallel().forEach(Startable::stop);
        started = false;
    }

    public int getFrontendPort() {
        return frontend.getPort();
    }

    public HermesTestClient api() {
        return hermesTestClient;
    }

    public HermesInitHelper initHelper() {
        return hermesInitHelper;
    }

    public void cutOffConnectionsBetweenBrokersAndClients() {
        kafka.cutOffConnectionsBetweenBrokersAndClients();
    }

    public void restoreConnectionsBetweenBrokersAndClients() {
        kafka.restoreConnectionsBetweenBrokersAndClients();
    }
}
