package pl.allegro.tech.hermes.integration;

import org.mockito.Mockito;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.frontend.server.SslContextFactory;
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
                .overrideProperty(Configs.FRONTEND_SSL_ENABLED, false);

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