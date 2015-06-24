package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeClass;
import pl.allegro.tech.hermes.integration.env.HermesIntegrationEnvironment;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.integration.helper.Waiter;

import static pl.allegro.tech.hermes.integration.env.SharedServices.services;

public class IntegrationTest extends HermesIntegrationEnvironment {

    protected HermesEndpoints management;

    protected HermesPublisher publisher;

    protected HermesAPIOperations operations;

    protected Waiter wait;

    @BeforeClass
    public void initializeIntegrationTest() {
        this.management = new HermesEndpoints(MANAGEMENT_ENDPOINT_URL);
        this.publisher = new HermesPublisher(FRONTEND_URL);
        this.operations = new HermesAPIOperations(management);
        this.wait = new Waiter(management, services().zookeeper(), services().kafkaZookeeper());
    }

}
