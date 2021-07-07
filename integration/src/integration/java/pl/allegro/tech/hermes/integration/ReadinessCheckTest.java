package pl.allegro.tech.hermes.integration;

import com.google.common.io.Files;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.DatacenterReadiness;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.env.CustomKafkaStarter;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.integration.env.ManagementStarter;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.endpoint.JerseyClientFactory;
import pl.allegro.tech.hermes.test.helper.environment.KafkaStarter;
import pl.allegro.tech.hermes.test.helper.environment.ZookeeperStarter;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class ReadinessCheckTest extends IntegrationTest {

    private static final int ZOOKEEPER_PORT = 14193;
    private static final String ZOOKEEPER_URL = "localhost:" + ZOOKEEPER_PORT;
    private static final int MANAGEMENT_PORT = 18083;
    private static final String MANAGEMENT_URL = "http://localhost:" + MANAGEMENT_PORT + "/";
    private static final int KAFKA_PORT = 9096;
    private static final String KAFKA_URL = "localhost:" + KAFKA_PORT;

    private ZookeeperStarter zookeeperStarter;
    private HermesAPIOperations operations;
    private ManagementStarter managementStarter;
    private KafkaStarter kafkaStarter;

    @BeforeClass
    public void setupEnvironment() throws Exception {
        zookeeperStarter = setupZookeeper();
        kafkaStarter = setupKafka();
        managementStarter = setupManagement();
        operations = setupOperations();
    }

    @AfterClass
    public void cleanEnvironment() throws Exception {
        managementStarter.stop();
        kafkaStarter.stop();
        zookeeperStarter.stop();
    }

    @Test
    public void shouldReturnCorrectReadinessStatusSetForParticularDC() throws Exception {
        // given
        int frontendPort = Ports.nextAvailable();
        String frontendUrl = "http://localhost:" + frontendPort + "/";

        operations.setReadiness("dc5", false);
        FrontendStarter frontend = setupFrontend(frontendPort); // frontend needs to be started after the flag is set in zookeeper

        // when
        WebTarget clientDcToTurnOff = JerseyClientFactory.create().target(frontendUrl).path("status").path("ready");
        WebTarget clientDefaultDc = JerseyClientFactory.create().target(FRONTEND_URL).path("status").path("ready");

        // then
        assertThat(clientDcToTurnOff.request().get()).hasStatus(SERVICE_UNAVAILABLE);
        assertThat(clientDefaultDc.request().get()).hasStatus(OK);

        // cleanup
        frontend.stop();

        // given
        operations.setReadiness("dc5", true);
        frontend = setupFrontend(frontendPort);

        // when
        clientDcToTurnOff = JerseyClientFactory.create().target(frontendUrl).path("status").path("ready");

        // then
        assertThat(clientDcToTurnOff.request().get()).hasStatus(OK);
        assertThat(clientDefaultDc.request().get()).hasStatus(OK);

        // cleanup
        frontend.stop();
    }

    @Test
    public void shouldReturnCorrectReadinessStatusBasedOnKafkaReadiness() throws Exception {
        // given
        int frontendPort = Ports.nextAvailable();
        String frontendUrl = "http://localhost:" + frontendPort + "/";
        FrontendStarter frontend = setupFrontend(frontendPort);

        operations.createGroup("someRandomGroup");
        operations.createTopic("someRandomGroup", "someRandomTopic");

        // when
        WebTarget clientReadiness = JerseyClientFactory.create().target(frontendUrl).path("status").path("ready");
        WebTarget clientHealth = JerseyClientFactory.create().target(frontendUrl).path("status").path("health");

        kafkaStarter.stop();

        // then
        assertThat(clientReadiness.request().get()).hasStatus(SERVICE_UNAVAILABLE);
        assertThat(clientHealth.request().get()).hasStatus(OK);

        // when
        kafkaStarter.start();

        // then
        assertThat(clientReadiness.request().get()).hasStatus(OK);
        assertThat(clientHealth.request().get()).hasStatus(OK);

        // cleanup
        frontend.stop();
    }

    @Test
    public void shouldReturnDatacentersWithItsZookeeperReadinessStatus() throws Exception {
        // given
        operations.setReadiness("dc5", false);

        // when
        WebTarget client = JerseyClientFactory.create().target(MANAGEMENT_URL).path("readiness").path("datacenters");
        Response response = client.request().get();

        // then
        assertThat(response).hasStatus(OK);
        assertThat(response.readEntity(new GenericType<List<DatacenterReadiness>>() {}))
                .contains(new DatacenterReadiness("dc", true))
                .contains(new DatacenterReadiness("dc5", false));

    }

    private FrontendStarter setupFrontend(int port) throws Exception {
        FrontendStarter frontend = new FrontendStarter(port, false);
        frontend.overrideProperty(Configs.FRONTEND_PORT, port);
        frontend.overrideProperty(Configs.FRONTEND_HTTP2_ENABLED, false);
        frontend.overrideProperty(Configs.FRONTEND_SSL_ENABLED, false);
        frontend.overrideProperty(Configs.METRICS_GRAPHITE_REPORTER, false);
        frontend.overrideProperty(Configs.METRICS_ZOOKEEPER_REPORTER, false);
        frontend.overrideProperty(Configs.MESSAGES_LOCAL_STORAGE_ENABLED, false);
        frontend.overrideProperty(Configs.MESSAGES_LOCAL_STORAGE_DIRECTORY, Files.createTempDir().getAbsolutePath());
        frontend.overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, ZOOKEEPER_URL);
        frontend.overrideProperty(Configs.FRONTEND_STARTUP_WAIT_KAFKA_ENABLED, false);
        frontend.overrideProperty(Configs.KAFKA_BROKER_LIST, KAFKA_URL);
        frontend.overrideProperty(Configs.FRONTEND_KAFKA_HEALTH_CHECK_WAIT_TIMEOUT, 500L);
        frontend.overrideProperty(Configs.FRONTEND_KAFKA_HEALTH_CHECK_INTERVAL, 200L);
        frontend.start();
        return frontend;
    }

    private ZookeeperStarter setupZookeeper() throws Exception {
        ZookeeperStarter zookeeperStarter = new ZookeeperStarter(ZOOKEEPER_PORT, ZOOKEEPER_URL, CONFIG_FACTORY.getStringProperty(Configs.ZOOKEEPER_ROOT));
        zookeeperStarter.start();
        return zookeeperStarter;
    }

    private ManagementStarter setupManagement() throws Exception {
        ManagementStarter managementStarter = new ManagementStarter(MANAGEMENT_PORT, "multizk");
        managementStarter.start();
        return managementStarter;
    }

    private HermesAPIOperations setupOperations() {
        HermesEndpoints management = new HermesEndpoints(MANAGEMENT_URL, CONSUMER_ENDPOINT_URL);
        return new HermesAPIOperations(management, wait);
    }

    private CustomKafkaStarter setupKafka() throws Exception {
        CustomKafkaStarter kafkaStarter = new CustomKafkaStarter(KAFKA_PORT, ZOOKEEPER_URL + "/unhealthyKafka");
        kafkaStarter.start();
        return kafkaStarter;
    }
}
