package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeClass;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.env.HermesIntegrationEnvironment;
import pl.allegro.tech.hermes.integration.helper.Waiter;
import pl.allegro.tech.hermes.test.helper.endpoint.BrokerOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;

import java.util.Arrays;

import static pl.allegro.tech.hermes.integration.env.SharedServices.services;

public class IntegrationTest extends HermesIntegrationEnvironment {

    protected HermesEndpoints management;

    protected HermesPublisher publisher;

    protected HermesAPIOperations operations;

    protected Waiter wait;

    protected BrokerOperations brokerOperations;

    @BeforeClass
    public void initializeIntegrationTest() {
        this.management = new HermesEndpoints(MANAGEMENT_ENDPOINT_URL);
        this.publisher = new HermesPublisher(FRONTEND_URL);
        this.wait = new Waiter(management, services().zookeeper(), services().kafkaZookeeper(), KAFKA_NAMESPACE);
        this.operations = new HermesAPIOperations(management, wait);
        this.brokerOperations = new BrokerOperations(Arrays.asList(PRIMARY_ZK_KAFKA_CONNECT, SECONDARY_ZK_KAFKA_CONNECT),
                CONFIG_FACTORY.getIntProperty(Configs.ZOOKEEPER_SESSION_TIMEOUT),
                CONFIG_FACTORY.getIntProperty(Configs.ZOOKEEPER_CONNECTION_TIMEOUT));
    }

}