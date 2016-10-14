package pl.allegro.tech.hermes.integration.env;

import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryRestApplication;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.internal.ServiceFinder;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

import java.util.Properties;

public class ConfluentSchemaRegistryStarter implements Starter<SchemaRegistryRestApplication> {

    private final Properties properties;
    private SchemaRegistryRestApplication restApp;
    private Server server;

    public ConfluentSchemaRegistryStarter(int port, String zkConnectionString) {
        Properties properties = new Properties();
        properties.put("listeners", "http://0.0.0.0:" + port);
        properties.put("kafkastore.connection.url", zkConnectionString);
        this.properties = properties;
    }

    @Override
    public void start() throws Exception {

        // temporarily overriding Jersey's default ServiceIteratorProvider
        // in order to filter out SpringComponentProvider (available in classpath)
        // which tries to initialize custom application context from an XML file
        ServiceFinder.setIteratorProvider(SpringlessServiceIteratorProvider.INSTANCE);

        SchemaRegistryConfig config = new SchemaRegistryConfig(properties);
        restApp = new SchemaRegistryRestApplication(config);
        server = restApp.createServer();
        server.start();

        ServiceFinder.setIteratorProvider(null);
    }

    @Override
    public void stop() throws Exception {
        server.stop();
    }

    @Override
    public SchemaRegistryRestApplication instance() {
        return restApp;
    }
}
