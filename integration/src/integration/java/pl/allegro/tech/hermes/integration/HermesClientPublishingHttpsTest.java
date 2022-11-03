package pl.allegro.tech.hermes.integration;

import okhttp3.OkHttpClient;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.client.HermesClient;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.okhttp.OkHttpHermesSender;
import pl.allegro.tech.hermes.common.ssl.DefaultSslContextFactory;
import pl.allegro.tech.hermes.common.ssl.KeystoreProperties;
import pl.allegro.tech.hermes.common.ssl.SSLContextHolder;
import pl.allegro.tech.hermes.common.ssl.provided.ProvidedKeyManagersProvider;
import pl.allegro.tech.hermes.common.ssl.provided.ProvidedTrustManagersProvider;
import pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.net.URI;
import javax.net.ssl.X509TrustManager;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_HTTP2_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_KEYSTORE_SOURCE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_PORT;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_TRUSTSTORE_SOURCE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.KAFKA_BROKER_LIST;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.ZOOKEEPER_CONNECTION_STRING;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class HermesClientPublishingHttpsTest extends IntegrationTest {

    private Topic topic;

    @BeforeClass
    public void setup() {
        topic = randomTopic("hermesClientHttpsGroup", "topic").build();
        operations.buildTopic(topic);
    }

    @Test
    public void shouldCommunicateWithHermesUsingHttp2() throws Exception {
        // given
        int port = Ports.nextAvailable();
        int sslPort = Ports.nextAvailable();

        FrontendStarter frontend = startFrontend(port, sslPort);
        String message = TestMessage.of("hello", "world").body();

        OkHttpHermesSender okHttpHermesSender = new OkHttpHermesSender(getOkHttpClientWithSslContextConfigured());
        HermesClient client = hermesClient(okHttpHermesSender)
                .withURI(URI.create("https://localhost:" + sslPort))
                .build();

        try {
            // when
            HermesResponse response = client.publish(topic.getQualifiedName(), message).join();

            // then
            assertThat(response.getProtocol()).isEqualTo("h2");
            assertThat(response.isSuccess()).isTrue();
        } finally {
            frontend.stop();
        }
    }

    private OkHttpClient getOkHttpClientWithSslContextConfigured() {
        String protocol = "TLS";
        KeystoreProperties keystoreProperties = new KeystoreProperties("classpath:client.keystore", "JKS", "password");
        KeystoreProperties truststoreProperties = new KeystoreProperties("classpath:client.truststore", "JKS", "password");
        DefaultSslContextFactory sslContextFactory = new DefaultSslContextFactory(
                protocol,
                new ProvidedKeyManagersProvider(keystoreProperties),
                new ProvidedTrustManagersProvider(truststoreProperties)
        );
        SSLContextHolder sslContextHolder = sslContextFactory.create();
        return new OkHttpClient.Builder()
                .sslSocketFactory(
                        sslContextHolder.getSslContext().getSocketFactory(),
                        (X509TrustManager) sslContextHolder.getTrustManagers()[0]
                )
                .build();
    }

    private FrontendStarter startFrontend(int port, int sslPort) throws Exception {
        FrontendStarter frontend = FrontendStarter.withCommonIntegrationTestConfig(port, true);

        frontend.overrideProperty(FRONTEND_HTTP2_ENABLED, true);
        frontend.overrideProperty(FRONTEND_SSL_PORT, sslPort);
        frontend.overrideProperty(FRONTEND_SSL_KEYSTORE_SOURCE, "provided");
        frontend.overrideProperty(FRONTEND_SSL_TRUSTSTORE_SOURCE, "provided");
        frontend.overrideProperty(KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients());
        frontend.overrideProperty(ZOOKEEPER_CONNECTION_STRING, hermesZookeeperOne.getConnectionString());
        frontend.overrideProperty(SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());

        frontend.start();

        return frontend;
    }
}
