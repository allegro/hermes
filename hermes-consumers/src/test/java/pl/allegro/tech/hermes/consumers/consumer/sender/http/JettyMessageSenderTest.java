package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.HttpCookieStore;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.common.http.MessageMetadataHeaders;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.PARENT_SPAN_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SPAN_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_REPORTED;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TRACE_SAMPLED;
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.withTestMessage;

@RunWith(MockitoJUnitRunner.class)
public class JettyMessageSenderTest {

    private static final String MESSAGE_BODY = "aaaaaaaaaaaaaaaa";
    private static final Message SOME_MESSAGE = withTestMessage()
            .withContent(MESSAGE_BODY, StandardCharsets.UTF_8)
            .build();
    private static final int ENDPOINT_PORT = 23215;
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
        client.setExecutor(Executors.newFixedThreadPool(10));
        client.setCookieStore(new HttpCookieStore.Empty());
        client.start();
    }

    @AfterClass
    public static void cleanEnvironment() {
        wireMockServer.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        remoteServiceEndpoint = new RemoteServiceEndpoint(wireMockServer);
        ResolvableEndpointAddress address = new ResolvableEndpointAddress(ENDPOINT, new SimpleEndpointAddressResolver());
        messageSender = new JettyMessageSender(client, address, 1000, new DefaultHttpTraceAppender());
    }

    @Test
    public void shouldSendMessageSuccessfully() throws Exception {
        // given
        remoteServiceEndpoint.expectMessages(MESSAGE_BODY);

        // when
        CompletableFuture<MessageSendingResult> future = messageSender.send(SOME_MESSAGE);

        // then
        remoteServiceEndpoint.waitUntilReceived();
        assertTrue(future.get(1, TimeUnit.SECONDS).succeeded());
    }

    @Test
    public void shouldReturnTrueWhenOtherSuccessfulCodeReturned() throws Exception {
        // given
        remoteServiceEndpoint.setReturnedStatusCode(ACCEPTED.getStatusCode());
        remoteServiceEndpoint.expectMessages(MESSAGE_BODY);

        // when
        CompletableFuture<MessageSendingResult> future = messageSender.send(SOME_MESSAGE);

        // then
        assertTrue(future.get(1, TimeUnit.SECONDS).succeeded());
    }

    @Test
    public void shouldReturnFalseWhenSendingFails() throws Exception {
        // given
        remoteServiceEndpoint.setReturnedStatusCode(INTERNAL_SERVER_ERROR.getStatusCode());
        remoteServiceEndpoint.expectMessages(MESSAGE_BODY);

        // when
        CompletableFuture<MessageSendingResult> future = messageSender.send(SOME_MESSAGE);

        // then
        remoteServiceEndpoint.waitUntilReceived();
        assertFalse(future.get(1, TimeUnit.SECONDS).succeeded());
    }


    @Test
    public void shouldSendMessageIdHeader() {
        // given
        remoteServiceEndpoint.expectMessages(MESSAGE_BODY);

        // when
        messageSender.send(SOME_MESSAGE);

        // then
        remoteServiceEndpoint.waitUntilReceived();
        assertThat(headerValue(remoteServiceEndpoint.getLastReceivedRequest(), MESSAGE_ID)).isEqualTo("id");
    }

    @Test
    public void shouldSendTraceIdHeader() {
        // given
        remoteServiceEndpoint.expectMessages(MESSAGE_BODY);

        // when
        messageSender.send(SOME_MESSAGE);

        // then
        remoteServiceEndpoint.waitUntilReceived();
        assertThat(headerValue(remoteServiceEndpoint.getLastReceivedRequest(), TRACE_ID)).isEqualTo("traceId");
    }

    @Test
    public void shouldSendTraceHeaders() {
        // given
        remoteServiceEndpoint.expectMessages(MESSAGE_BODY);

        // when
        messageSender.send(SOME_MESSAGE);

        // then
        remoteServiceEndpoint.waitUntilReceived();
        LoggedRequest lastRequest = remoteServiceEndpoint.getLastReceivedRequest();
        assertThat(headerValue(lastRequest, TRACE_ID)).isEqualTo("traceId");
        assertThat(headerValue(lastRequest, SPAN_ID)).isEqualTo("spanId");
        assertThat(headerValue(lastRequest, PARENT_SPAN_ID)).isEqualTo("parentSpanId");
        assertThat(headerValue(lastRequest, TRACE_SAMPLED)).isEqualTo("traceSampled");
        assertThat(headerValue(lastRequest, TRACE_REPORTED)).isEqualTo("traceReported");
    }

    private static String headerValue(LoggedRequest lastRequest, MessageMetadataHeaders header) {
        return lastRequest.getHeader(header.getName());
    }

    private static final class SimpleEndpointAddressResolver implements EndpointAddressResolver {
        @Override
        public URI resolve(EndpointAddress address, Message message) {
            return URI.create(address.getEndpoint());
        }
    }
}
