package pl.allegro.tech.hermes.integration;

import com.google.common.io.Files;
import org.mockito.Mockito;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.frontend.server.SslContextFactoryProvider;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomSslContextFactoryTest extends IntegrationTest {

    public static final int FRONTEND_PORT = Ports.nextAvailable();

    private HermesFrontend hermesFrontend;

    @Test
    public void shouldInjectCustomSslContextFactoryToFrontend() {
        // given
        SslContextFactory customSslContextFactory = Mockito.mock(SslContextFactory.class);

        ConfigFactory configFactory = new MutableConfigFactory()
                .overrideProperty(Configs.FRONTEND_PORT, FRONTEND_PORT)
                .overrideProperty(Configs.FRONTEND_SSL_ENABLED, false)
                .overrideProperty(Configs.KAFKA_AUTHORIZATION_ENABLED, false)
                .overrideProperty(Configs.KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients())
                .overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, hermesZookeeperOne.getConnectionString())
                .overrideProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl())
                .overrideProperty(Configs.MESSAGES_LOCAL_STORAGE_DIRECTORY, Files.createTempDir().getAbsolutePath());

        hermesFrontend = HermesFrontend.frontend()
                .withBinding(configFactory, ConfigFactory.class)
                .withSslContextFactory(customSslContextFactory)
                .build();
        hermesFrontend.start();

        // when
        SslContextFactoryProvider sslContextFactoryProvider = hermesFrontend.getService(SslContextFactoryProvider.class);

        // then
        assertThat(sslContextFactoryProvider.getSslContextFactory()).isEqualTo(customSslContextFactory);
    }

    @AfterClass
    public void tearDown() throws InterruptedException {
        hermesFrontend.stop();
    }
}