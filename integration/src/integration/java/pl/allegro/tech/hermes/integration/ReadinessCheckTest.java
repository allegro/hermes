package pl.allegro.tech.hermes.integration;

import com.google.common.io.Files;
import javax.ws.rs.client.WebTarget;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.integration.env.ManagementStarter;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.endpoint.JerseyClientFactory;
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

    private ZookeeperStarter zookeeperStarter;
    private ManagementStarter managementStarter;
    private HermesAPIOperations operations;

    @BeforeClass
    public void setupEnvironment() throws Exception {
        zookeeperStarter = setupZookeeper();
        managementStarter = setupManagement();
        operations = setupOperations();
    }

    @AfterClass
    public void cleanEnvironment() throws Exception {
        zookeeperStarter.stop();
        managementStarter.stop();
    }

    @Test
    public void shouldReturnCorrectHealthStatusForParticularDC() throws Exception {
        // given
        int frontendPort = Ports.nextAvailable();
        String frontendUrl = "http://localhost:" + frontendPort + "/";

        operations.setReadiness("dc5", false);
        FrontendStarter frontendStarter = setupFrontend(frontendPort); // frontend needs to be started after the flag is set in zookeeper

        // when
        WebTarget clientDc5 = JerseyClientFactory.create().target(frontendUrl).path("status").path("ready");
        WebTarget clientDefaultDc = JerseyClientFactory.create().target(FRONTEND_URL).path("status").path("ready");

        // then
        assertThat(clientDc5.request().get()).hasStatus(SERVICE_UNAVAILABLE);
        assertThat(clientDefaultDc.request().get()).hasStatus(OK);

        // cleanup
        frontendStarter.stop();

        // given
        operations.setReadiness("dc5", true);
        frontendStarter = setupFrontend(frontendPort);

        // when
        clientDc5 = JerseyClientFactory.create().target(frontendUrl).path("status").path("ready");

        // then
        assertThat(clientDc5.request().get()).hasStatus(OK);
        assertThat(clientDefaultDc.request().get()).hasStatus(OK);

        // cleanup
        frontendStarter.stop();
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
}
