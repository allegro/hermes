package pl.allegro.tech.hermes.integration.env;

import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryRestApplication;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.internal.ServiceFinder;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

import java.util.Properties;

public class ConfluentSchemaRegistryStarter implements Starter<SchemaRegistryRestApplication> {

//    private final Properties properties;
//    private SchemaRegistryRestApplication restApp;
//    private Server server;

    private final FixedHostPortGenericContainer schemaRegistryContainer;

//    schema-registry:
//    image: confluentinc/cp-schema-registry:5.1.0
//    depends_on:
//            - kafka
//      - zk
//    environment:
//    SCHEMA_REGISTRY_HOST_NAME: schema-registry
//    SCHEMA_REGISTRY_KAFKASTORE_CONNECTION_URL: zk:2181
//    SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081
//    ports:
//            - "8081:8081"

    public ConfluentSchemaRegistryStarter(Network network, int port, int zkPort) {
        this.schemaRegistryContainer = new FixedHostPortGenericContainer<>("confluentinc/cp-schema-registry:5.1.0")
                .withNetwork(network)
                .withCreateContainerCmdModifier(it -> it.withName("schema-registry"))
                .withExposedPorts(8081)
                .withFixedExposedPort(port, 8081)
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_CONNECTION_URL", String.format("zookeeper:%s/secondaryKafka", zkPort))
                .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                .waitingFor(Wait.defaultWaitStrategy());
    }

    @Override
    public void start() throws Exception {

        // temporarily overriding Jersey's default ServiceIteratorProvider
        // in order to filter out SpringComponentProvider (available in classpath)
        // which tries to initialize custom application context from an XML file
//        ServiceFinder.setIteratorProvider(SpringlessServiceIteratorProvider.INSTANCE);

//        SchemaRegistryConfig config = new SchemaRegistryConfig(properties);
//        restApp = new SchemaRegistryRestApplication(config);
//        server = restApp.createServer();
//        server.start();

        schemaRegistryContainer.start();

//        ServiceFinder.setIteratorProvider(null);
    }

    @Override
    public void stop() throws Exception {
//        server.stop();
        schemaRegistryContainer.stop();
    }

    @Override
    public SchemaRegistryRestApplication instance() {
        return null;
    }
}
