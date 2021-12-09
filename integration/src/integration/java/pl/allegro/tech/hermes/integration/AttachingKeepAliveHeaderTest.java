package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.core.Response;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_KEEP_ALIVE_HEADER_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_KEEP_ALIVE_HEADER_TIMEOUT_SECONDS;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_STORAGE_ENABLED;

public class AttachingKeepAliveHeaderTest extends IntegrationTest {

    private static final int FRONTEND_PORT = Ports.nextAvailable();
    private static final String FRONTEND_URL = "http://127.0.0.1:" + FRONTEND_PORT;
    private static final String MESSAGE = TestMessage.of("hello", "world").body();

    @BeforeClass
    public void setup() {
        publisher = new HermesPublisher(FRONTEND_URL);
        operations.buildTopic("someGroup", "topicWithKeepAlive");
    }

    @Test
    public void shouldAttachKeepAliveHeaderWhenEnabled() throws Exception {
        //given
        FrontendStarter hermesFrontend = startFrontendWithConfig(frontend -> {
            frontend.overrideProperty(FRONTEND_KEEP_ALIVE_HEADER_ENABLED, true);
            frontend.overrideProperty(FRONTEND_KEEP_ALIVE_HEADER_TIMEOUT_SECONDS, 2);
        });

        try {
            //when
            Response response = publisher.publish("someGroup.topicWithKeepAlive", MESSAGE);

            //then
            assertThat(response.getHeaderString("Keep-Alive")).isEqualTo("timeout=2");
        } finally {
            hermesFrontend.stop();
        }
    }

    @Test
    public void shouldNotAttachKeepAliveHeaderWhenDisabled() throws Exception {
        //given
        FrontendStarter hermesFrontend = startFrontendWithConfig(frontend -> frontend
                .overrideProperty(FRONTEND_KEEP_ALIVE_HEADER_ENABLED, false)
        );

        try {
            //when
            Response response = publisher.publish("someGroup.topicWithKeepAlive", MESSAGE);

            //then
            assertThat(response.getHeaderString("Keep-Alive")).isNull();
        } finally {
            hermesFrontend.stop();
        }
    }

    private FrontendStarter startFrontendWithConfig(Consumer<FrontendStarter> frontendConfigUpdater) throws Exception {
        FrontendStarter frontend = new FrontendStarter(FRONTEND_PORT, false);
        frontend.overrideProperty(MESSAGES_LOCAL_STORAGE_ENABLED, false);
        frontend.overrideProperty(Configs.KAFKA_AUTHORIZATION_ENABLED, false);
        frontend.overrideProperty(Configs.KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients());
        frontend.overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, hermesZookeeperOne.getConnectionString());
        frontend.overrideProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
        frontendConfigUpdater.accept(frontend);
        frontend.start();
        return frontend;
    }
}
