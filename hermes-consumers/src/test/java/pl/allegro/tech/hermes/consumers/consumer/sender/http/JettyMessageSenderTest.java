package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import com.codahale.metrics.MetricRegistry;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.eclipse.jetty.client.HttpClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.AuthHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HermesHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.Http1HeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.SimpleEndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.test.MessageBuilder;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.TEST_MESSAGE_CONTENT;
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.testMessage;

public class JettyMessageSenderTest {

    private static final int ENDPOINT_PORT = Ports.nextAvailable();
    private static final EndpointAddress ENDPOINT = EndpointAddress.of(format("http://localhost:%d/", ENDPOINT_PORT));
    private static final EndpointAddressResolverMetadata METADATA = EndpointAddressResolverMetadata.empty();

    private static HttpClient client;
    private static WireMockServer wireMockServer;

    private ResolvableEndpointAddress address;
    private RemoteServiceEndpoint remoteServiceEndpoint;
    private JettyMessageSender messageSender;

    private HttpHeadersProvider headersProvider = new HermesHeadersProvider(new Http1HeadersProvider());

    @BeforeClass
    public static void setupEnvironment() throws Exception {
        wireMockServer = new WireMockServer(ENDPOINT_PORT);
        wireMockServer.start();

        ConfigFactory configFactory = new MutableConfigFactory();
        SslContextFactoryProvider sslContextFactoryProvider = new SslContextFactoryProvider();
        sslContextFactoryProvider.configFactory = configFactory;

        HttpClientFactory httpClientFactory = new HttpClientFactory(new HttpClientsFactory(
                configFactory,
                new InstrumentedExecutorServiceFactory(new HermesMetrics(new MetricRegistry(), new PathsCompiler("localhost"))),
                sslContextFactoryProvider));

        client = httpClientFactory.provide();
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
        address = new ResolvableEndpointAddress(ENDPOINT, new SimpleEndpointAddressResolver(), METADATA);
        HttpRequestFactory httpRequestFactory = new HttpRequestFactory(client, 1000, 1000, new DefaultHttpMetadataAppender(), headersProvider);
        messageSender = new JettyMessageSender(httpRequestFactory, address);
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
    public void shouldNotRedirectMessage() throws InterruptedException, ExecutionException, TimeoutException {
        // given
        int numberOfExpectedMessages = 1;
        int maximumWaitTimeInSeconds = 1;
        remoteServiceEndpoint.redirectMessage(TEST_MESSAGE_CONTENT);

        // when
        CompletableFuture<MessageSendingResult> future = messageSender.send(testMessage());

        // then
        assertThat(future.get(maximumWaitTimeInSeconds, TimeUnit.SECONDS).succeeded()).isFalse();
        remoteServiceEndpoint.waitUntilReceived(maximumWaitTimeInSeconds, numberOfExpectedMessages);
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
    public void shouldSendRetryCounterInHeader() throws InterruptedException, ExecutionException, TimeoutException {
        // given
        remoteServiceEndpoint.expectMessages(TEST_MESSAGE_CONTENT);

        // when
        CompletableFuture<MessageSendingResult> result = messageSender.send(testMessage());

        // then
        assertThat(result.get(1000, TimeUnit.MILLISECONDS).getStatusCode()).isEqualTo(200);
        remoteServiceEndpoint.waitUntilReceived();
        assertThat(remoteServiceEndpoint.getLastReceivedRequest().getHeader("Hermes-Retry-Count")).isEqualTo("0");
    }

    @Test
    public void shouldSendAuthorizationHeaderIfAuthorizationProviderAttached() {
        // given
        HttpRequestFactory httpRequestFactory = new HttpRequestFactory(client, 1000, 1000, new DefaultHttpMetadataAppender(),
                new HermesHeadersProvider(
                        new AuthHeadersProvider(
                                new Http1HeadersProvider(),
                                () -> Optional.of("Basic Auth Hello!")
                        )
                ));

        JettyMessageSender messageSender = new JettyMessageSender(httpRequestFactory, address);
        Message message = MessageBuilder.withTestMessage().build();
        remoteServiceEndpoint.expectMessages(TEST_MESSAGE_CONTENT);

        // when
        messageSender.send(message);

        // then
        remoteServiceEndpoint.waitUntilReceived();
        assertThat(remoteServiceEndpoint.getLastReceivedRequest().getHeader("Authorization")).isEqualTo("Basic Auth Hello!");
    }

    @Test
    public void shouldUseSuppliedRequestTimeout() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        HttpRequestFactory httpRequestFactory = new HttpRequestFactory(client,
                100, 1000,
                new DefaultHttpMetadataAppender(),
                headersProvider);
        remoteServiceEndpoint.setDelay(500);

        JettyMessageSender messageSender = new JettyMessageSender(httpRequestFactory, address);
        Message message = MessageBuilder.withTestMessage().build();
        remoteServiceEndpoint.expectMessages(TEST_MESSAGE_CONTENT);

        // when
        MessageSendingResult messageSendingResult = messageSender.send(message).get(1000, TimeUnit.MILLISECONDS);

        // then
        assertThat(messageSendingResult.isTimeout()).isTrue();
    }

    @Test
    public void shouldUseSuppliedSocketTimeout() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        HttpRequestFactory httpRequestFactory = new HttpRequestFactory(client,
                1000, 100,
                new DefaultHttpMetadataAppender(),
                headersProvider);
        remoteServiceEndpoint.setDelay(200);

        JettyMessageSender messageSender = new JettyMessageSender(httpRequestFactory, address);
        Message message = MessageBuilder.withTestMessage().build();
        remoteServiceEndpoint.expectMessages(TEST_MESSAGE_CONTENT);

        // when
        MessageSendingResult messageSendingResult = messageSender.send(message).get(1000, TimeUnit.MILLISECONDS);

        // then
        assertThat(messageSendingResult.isTimeout()).isTrue();
    }
}
