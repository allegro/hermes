package pl.allegro.tech.hermes.integrationtests;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.client.HermesClient;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.okhttp.OkHttpHermesSender;
import pl.allegro.tech.hermes.common.ssl.DefaultSslContextFactory;
import pl.allegro.tech.hermes.common.ssl.KeystoreProperties;
import pl.allegro.tech.hermes.common.ssl.SSLContextHolder;
import pl.allegro.tech.hermes.common.ssl.provided.ProvidedKeyManagersProvider;
import pl.allegro.tech.hermes.common.ssl.provided.ProvidedTrustManagersProvider;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesInitHelper;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementTestApp;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_HTTP2_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_KEYSTORE_SOURCE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_PORT;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_TRUSTSTORE_SOURCE;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class HermesClientPublishingHttpsTest {

    private Topic topic;

    private static final ZookeeperContainer hermesZookeeper = new ZookeeperContainer("HermesZookeeper");
    private static final KafkaContainerCluster kafka = new KafkaContainerCluster(1);
    private static final ConfluentSchemaRegistryContainer schemaRegistry = new ConfluentSchemaRegistryContainer()
            .withKafkaCluster(kafka);
    private static final HermesManagementTestApp management = new HermesManagementTestApp(hermesZookeeper, kafka, schemaRegistry);
    private static HermesInitHelper initHelper;
    private static HermesFrontendTestApp frontend;
    private static final int frontendSSLPort = Ports.nextAvailable();

    @BeforeAll
    public static void setup() {
        Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::start);
        schemaRegistry.start();
        management.start();
        initHelper = new HermesInitHelper(management.getPort());

        frontend = new HermesFrontendTestApp(hermesZookeeper, kafka, schemaRegistry);

        frontend.withProperty(FRONTEND_SSL_ENABLED, true);
        frontend.withProperty(FRONTEND_HTTP2_ENABLED, true);
        frontend.withProperty(FRONTEND_SSL_PORT, frontendSSLPort);
        frontend.withProperty(FRONTEND_SSL_KEYSTORE_SOURCE, "provided");
        frontend.withProperty(FRONTEND_SSL_TRUSTSTORE_SOURCE, "provided");

        frontend.start();
    }

    @AfterAll
    public static void clean() {
        management.stop();
        Stream.of(hermesZookeeper, kafka, schemaRegistry)
                .parallel()
                .forEach(Startable::stop);
        frontend.stop();
    }


    @Test
    public void shouldCommunicateWithHermesUsingHttp2() throws Exception {
        // given
        Topic topic = initHelper.createTopic(topicWithRandomName().build());
        String message = TestMessage.of("hello", "world").body();

        OkHttpHermesSender okHttpHermesSender = new OkHttpHermesSender(getOkHttpClientWithSslContextConfigured());
        HermesClient client = hermesClient(okHttpHermesSender)
                .withURI(URI.create("https://localhost:" + frontendSSLPort))
                .build();

        // when
        HermesResponse response = client.publish(topic.getQualifiedName(), message).join();

        // then
        assertThat(response.getProtocol()).isEqualTo("h2");
        assertThat(response.isSuccess()).isTrue();
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

}
