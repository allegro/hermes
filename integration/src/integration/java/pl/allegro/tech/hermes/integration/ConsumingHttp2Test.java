package pl.allegro.tech.hermes.integration;

import io.undertow.Undertow;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.ssl.JvmKeystoreSslContextFactory;
import pl.allegro.tech.hermes.common.ssl.KeystoreProperties;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Response;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

import static io.undertow.UndertowOptions.ENABLE_HTTP2;
import static javax.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class ConsumingHttp2Test extends IntegrationTest {
    static final int HTTPS_PORT = Ports.nextAvailable();
    static final String HTTPS_ENDPOINT = "https://localhost:" + HTTPS_PORT;

    private AtomicInteger incomingCounter = new AtomicInteger(0);
    private Undertow http2Server;

    @Test
    public void shouldDeliverMessageUsingHttp2() throws InterruptedException {
        // given
        Topic topic = operations.buildTopic("deliverHttp2", "topic");

        Subscription subscription = SubscriptionBuilder.subscription(topic, "subscription")
                .withEndpoint(HTTPS_ENDPOINT)
                .withHttp2Enabled(true)
                .build();

        operations.createSubscription(topic, subscription);

        // when
        Response response = publisher.publish(topic.getQualifiedName(), TestMessage.of("hello", "h2").body());

        // then
        assertThat(response).hasStatus(CREATED);
        wait.until(() -> assertThat(incomingCounter.intValue()).isPositive());
    }

    @BeforeMethod
    public void beforeMethod() {
        incomingCounter.set(0);
    }

    @BeforeClass
    public void before() throws NoSuchAlgorithmException {
        this.http2Server = Undertow.builder()
                .addHttpsListener(HTTPS_PORT, "localhost", getSslContext())
                .setServerOption(ENABLE_HTTP2, true)
                .setHandler(exchange -> {
                    exchange.getResponseSender().send(Integer.toString(incomingCounter.incrementAndGet()));
                })
                .build();
        http2Server.start();
    }

    private SSLContext getSslContext() {
        KeystoreProperties keystore = new KeystoreProperties("classpath:server.keystore", "JKS", "password");
        KeystoreProperties truststore = new KeystoreProperties("classpath:server.truststore", "JKS", "password");
        return new JvmKeystoreSslContextFactory("TLS", keystore, truststore).create();
    }

    @AfterClass
    public void after() {
        http2Server.stop();
    }
}
