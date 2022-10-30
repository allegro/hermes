package pl.allegro.tech.hermes.integration;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.util.HttpString;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.ssl.DefaultSslContextFactory;
import pl.allegro.tech.hermes.common.ssl.KeystoreProperties;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.common.ssl.provided.ProvidedKeyManagersProvider;
import pl.allegro.tech.hermes.common.ssl.provided.ProvidedTrustManagersProvider;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Response;

import static io.undertow.UndertowOptions.ENABLE_HTTP2;
import static io.undertow.util.Protocols.HTTP_2_0;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.assertj.core.api.Assertions.fail;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class ConsumingHttp2Test extends IntegrationTest {

    private static final String MESSAGE_BODY = TestMessage.of("hello", "h2").body();

    private final AtomicInteger incomingCounter = new AtomicInteger(0);

    @Test
    public void shouldDeliverMessageUsingHttp2() {
        // given
        int httpsPort = Ports.nextAvailable();
        String httpsEndpoint = "https://localhost:" + httpsPort;

        Undertow http2Server = http2Server(httpsPort,
                exchange -> exchange.getResponseSender().send(Integer.toString(incomingCounter.incrementAndGet())));

        http2Server.start();

        Topic topic = createTopicAndSubscriptionWithHttp2Enabled(httpsEndpoint);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), MESSAGE_BODY);

        // then
        assertThat(response).hasStatus(CREATED);
        wait.until(() -> assertThat(incomingCounter.intValue()).isPositive());
    }

    @Test
    public void shouldDeliverMessageWithoutKeepAliveHeaderUsingHttp2() {
        // given
        int httpsPort = Ports.nextAvailable();
        HttpString keepAliveHeader = new HttpString("Keep-Alive");
        String httpsEndpoint = "https://localhost:" + httpsPort;

        Undertow http2Server = http2Server(httpsPort, exchange -> {
            if (!exchange.getProtocol().equals(HTTP_2_0)) {
                fail("Exchange protocol should be set to HTTP/2");
            }

            if (exchange.getRequestHeaders().contains(keepAliveHeader)) {
                fail("Keep-Alive header should not be present in HTTP/2 request headers");
            } else {
                exchange.getResponseSender().send(Integer.toString(incomingCounter.incrementAndGet()));
            }
        });

        http2Server.start();

        Topic topic = createTopicAndSubscriptionWithHttp2Enabled(httpsEndpoint);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), MESSAGE_BODY);

        // then
        assertThat(response).hasStatus(CREATED);
        wait.until(() -> assertThat(incomingCounter.intValue()).isPositive());

        http2Server.stop();
    }

    @BeforeMethod
    public void beforeMethod() {
        incomingCounter.set(0);
    }

    private Undertow http2Server(int httpsPort, HttpHandler handler) {
        return Undertow.builder()
                .addHttpsListener(httpsPort, "localhost", getSslContext())
                .setServerOption(ENABLE_HTTP2, true)
                .setHandler(handler)
                .build();
    }

    private Topic createTopicAndSubscriptionWithHttp2Enabled(String httpsEndpoint) {
        Topic topic = operations.buildTopic(randomTopic("deliverHttp2", "topic").build());

        Subscription subscription = SubscriptionBuilder.subscription(topic, "subscription")
                .withEndpoint(httpsEndpoint)
                .withHttp2Enabled(true)
                .build();

        operations.createSubscription(topic, subscription);
        return topic;
    }

    private SSLContext getSslContext() {
        String protocol = "TLS";
        KeystoreProperties keystoreProperties = new KeystoreProperties("classpath:server.keystore", "JKS", "password");
        KeystoreProperties truststoreProperties = new KeystoreProperties("classpath:server.truststore", "JKS", "password");
        SslContextFactory sslContextFactory = new DefaultSslContextFactory(
                protocol,
                new ProvidedKeyManagersProvider(keystoreProperties),
                new ProvidedTrustManagersProvider(truststoreProperties)
        );
        return sslContextFactory.create().getSslContext();
    }

}
