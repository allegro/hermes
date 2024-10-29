package pl.allegro.tech.hermes.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.client.HermesClientBuilder.hermesClient;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_HTTP2_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_KEYSTORE_SOURCE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_PORT;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_SSL_TRUSTSTORE_SOURCE;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.net.URI;
import java.time.Duration;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementExtension;
import pl.allegro.tech.hermes.integrationtests.setup.InfrastructureExtension;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class HermesClientPublishingHttpsTest {

  @Order(0)
  @RegisterExtension
  public static InfrastructureExtension infra = new InfrastructureExtension();

  @Order(1)
  @RegisterExtension
  public static HermesManagementExtension management = new HermesManagementExtension(infra);

  private static HermesFrontendTestApp frontend;

  @BeforeAll
  public static void setup() {
    frontend =
        new HermesFrontendTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());
    frontend.withProperty(FRONTEND_SSL_ENABLED, true);
    frontend.withProperty(FRONTEND_HTTP2_ENABLED, true);
    frontend.withProperty(FRONTEND_SSL_PORT, 0);
    frontend.withProperty(FRONTEND_SSL_KEYSTORE_SOURCE, "provided");
    frontend.withProperty(FRONTEND_SSL_TRUSTSTORE_SOURCE, "provided");

    frontend.start();
  }

  @AfterAll
  public static void clean() {
    frontend.stop();
  }

  @Test
  public void shouldCommunicateWithHermesUsingHttp2() {
    // given
    Topic topic = management.initHelper().createTopic(topicWithRandomName().build());
    String message = TestMessage.of("hello", "world").body();

    OkHttpHermesSender okHttpHermesSender =
        new OkHttpHermesSender(getOkHttpClientWithSslContextConfigured());
    HermesClient client =
        hermesClient(okHttpHermesSender)
            .withRetries(5)
            .withRetrySleep(Duration.ofSeconds(5).toMillis(), Duration.ofSeconds(10).toMillis())
            .withURI(URI.create("https://localhost:" + frontend.getSSLPort()))
            .build();

    // when
    HermesResponse response = client.publish(topic.getQualifiedName(), message).join();

    // then
    assertThat(response.getProtocol()).isEqualTo("h2");
    assertThat(response.isSuccess()).isTrue();
  }

  private OkHttpClient getOkHttpClientWithSslContextConfigured() {
    DefaultSslContextFactory sslContextFactory = getDefaultSslContextFactory();
    SSLContextHolder sslContextHolder = sslContextFactory.create();
    return new OkHttpClient.Builder()
        .sslSocketFactory(
            sslContextHolder.getSslContext().getSocketFactory(),
            (X509TrustManager) sslContextHolder.getTrustManagers()[0])
        .build();
  }

  private static DefaultSslContextFactory getDefaultSslContextFactory() {
    String protocol = "TLS";
    KeystoreProperties keystoreProperties =
        new KeystoreProperties("classpath:client.keystore", "JKS", "password");
    KeystoreProperties truststoreProperties =
        new KeystoreProperties("classpath:client.truststore", "JKS", "password");
    return new DefaultSslContextFactory(
        protocol,
        new ProvidedKeyManagersProvider(keystoreProperties),
        new ProvidedTrustManagersProvider(truststoreProperties));
  }
}
