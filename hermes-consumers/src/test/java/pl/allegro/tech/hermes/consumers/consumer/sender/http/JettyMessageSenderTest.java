package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.HttpCookieStore;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.SimpleEndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.test.MessageBuilder;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.TEST_MESSAGE_CONTENT;
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.testMessage;

public class JettyMessageSenderTest {

    private static final int ENDPOINT_PORT = Ports.nextAvailable();
    private static final EndpointAddress ENDPOINT = EndpointAddress.of(format("http://localhost:%d/", ENDPOINT_PORT));

    private static HttpClient client;
    private static WireMockServer wireMockServer;

    private RemoteServiceEndpoint remoteServiceEndpoint;
    private JettyMessageSender messageSender;

    @BeforeClass
    public static void setupEnvironment() throws Exception {
        wireMockServer = new WireMockServer(ENDPOINT_PORT);
        wireMockServer.start();

        client = new HttpClient();
        client.setCookieStore(new HttpCookieStore.Empty());
        client.setConnectTimeout(1000);
        client.setIdleTimeout(1000);
        client.start();
    }

    @AfterClass
    public static void cleanEnvironment() throws Exception {
        wireMockServer.shutdown();
        client.stop();
    }

    @Before
    public void setUp() throws Exception {
        remoteServiceEndpoint = new RemoteServiceEndpoint(wireMockServer);
        ResolvableEndpointAddress address = new ResolvableEndpointAddress(ENDPOINT, new SimpleEndpointAddressResolver());
        messageSender = new JettyMessageSender(client, address, 1000, new DefaultHttpMetadataAppender());
    }

    @Test
    public void shouldSendMessageSuccessfully() throws Exception {
        // given
        remoteServiceEndpoint.expectMessages(TEST_MESSAGE_CONTENT);

        // when
        CompletableFuture<MessageSendingResult> future = messageSender.send(testMessage());

        // then
        remoteServiceEndpoint.waitUntilReceived();
        assertThat(future.get(1, TimeUnit.SECONDS).succeeded()).isTrue();
    }

    @Test
    public void shouldReturnTrueWhenOtherSuccessfulCodeReturned() throws Exception {
        // given
        remoteServiceEndpoint.setReturnedStatusCode(ACCEPTED.getStatusCode());
        remoteServiceEndpoint.expectMessages(TEST_MESSAGE_CONTENT);

        // when
        CompletableFuture<MessageSendingResult> future = messageSender.send(testMessage());

        // then
        assertThat(future.get(1, TimeUnit.SECONDS).succeeded()).isTrue();
    }

    @Test
    public void shouldReturnFalseWhenSendingFails() throws Exception {
        // given
        remoteServiceEndpoint.setReturnedStatusCode(INTERNAL_SERVER_ERROR.getStatusCode());
        remoteServiceEndpoint.expectMessages(TEST_MESSAGE_CONTENT);

        // when
        CompletableFuture<MessageSendingResult> future = messageSender.send(testMessage());

        // then
        remoteServiceEndpoint.waitUntilReceived();
        assertThat(future.get(1, TimeUnit.SECONDS).succeeded()).isFalse();
    }


    @Test
    public void shouldSendMessageIdHeader() {
        // given
        remoteServiceEndpoint.expectMessages(TEST_MESSAGE_CONTENT);

        // when
        messageSender.send(testMessage());

        // then
        remoteServiceEndpoint.waitUntilReceived();
        assertThat(remoteServiceEndpoint.getLastReceivedRequest().getHeader("Hermes-Message-Id")).isEqualTo("id");
    }

    @Test
    public void shouldSendTraceIdHeader() {
        // given
        remoteServiceEndpoint.expectMessages(TEST_MESSAGE_CONTENT);

        // when
        messageSender.send(testMessage());

        // then
        remoteServiceEndpoint.waitUntilReceived();
        assertThat(remoteServiceEndpoint.getLastReceivedRequest().getHeader("Trace-Id")).isEqualTo("traceId");
    }

    @Test
    public void shouldSendRetryCounterInHeader() {
        // given
        Message message = MessageBuilder.withTestMessage().build();
        message.incrementRetryCounter();
        remoteServiceEndpoint.expectMessages(TEST_MESSAGE_CONTENT);

        // when
        messageSender.send(message);

        // then
        remoteServiceEndpoint.waitUntilReceived();
        assertThat(remoteServiceEndpoint.getLastReceivedRequest().getHeader("Hermes-Retry-Count")).isEqualTo("1");
    }
}
