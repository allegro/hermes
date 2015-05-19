package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.eclipse.jetty.client.api.Response.Listener.Adapter;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JettyMessageSenderTest {

    private static final Message SOME_MESSAGE = new Message(
            Optional.of("id"), 0, 0, "topic", "aaaaaaaaaaaaaaaa".getBytes(), Optional.of(2134144L), Optional.of(21341445L)
    );

    private static final EndpointAddress ENDPOINT = EndpointAddress.of("whatever");

    @Mock
    private HttpClient clientMock;

    @Mock
    private Request requestMock;

    @Mock
    private Result resultMock;

    @Mock
    private Response responseMock;

    private JettyMessageSender messageSender;

    @Before
    public void setUp() throws Exception {
        ResolvableEndpointAddress address = new ResolvableEndpointAddress(ENDPOINT, new SimpleEndpointAddressResolver());
        messageSender = new JettyMessageSender(clientMock, address, 1);

        when(clientMock.newRequest(any(URI.class))).thenReturn(requestMock);
        when(requestMock.method(any(HttpMethod.class))).thenReturn(requestMock);
        when(requestMock.header(anyString(), anyString())).thenReturn(requestMock);
        when(requestMock.timeout(anyInt(), any(TimeUnit.class))).thenReturn(requestMock);
        when(requestMock.content(any(ContentProvider.class))).thenReturn(requestMock);
    }

    @Test
    public void shouldReturnTrueWhenMessageSuccessfullySent() throws Exception {
        // given
        CompletableFuture<MessageSendingResult> future = new CompletableFuture<>();
        when(resultMock.isFailed()).thenReturn(false);
        when(resultMock.getResponse()).thenReturn(responseMock);
        when(responseMock.getStatus()).thenReturn(200);

        // when
        messageSender.sendMessage(SOME_MESSAGE, future);

        // then
        ArgumentCaptor<Adapter> adapterCaptor = ArgumentCaptor.forClass(Adapter.class);
        verify(requestMock).send(adapterCaptor.capture());
        adapterCaptor.getValue().onComplete(resultMock);
        assertTrue(future.get(1, TimeUnit.SECONDS).succeeded());
    }

    @Test
    public void shouldReturnTrueWhenOtherSuccessfulCodeReturned() throws Exception {
        // given
        CompletableFuture<MessageSendingResult> future = new CompletableFuture<>();
        when(resultMock.isFailed()).thenReturn(false);
        when(resultMock.getResponse()).thenReturn(responseMock);
        when(responseMock.getStatus()).thenReturn(203);

        // when
        messageSender.sendMessage(SOME_MESSAGE, future);

        // then
        ArgumentCaptor<Adapter> adapterCaptor = ArgumentCaptor.forClass(Adapter.class);
        verify(requestMock).send(adapterCaptor.capture());
        adapterCaptor.getValue().onComplete(resultMock);
        assertTrue(future.get(1, TimeUnit.SECONDS).succeeded());
    }

    @Test
    public void shouldReleaseSemaphoreWhenMessageSuccessfullySent() throws Exception {
        // given
        CompletableFuture<MessageSendingResult> future = new CompletableFuture<>();
        when(resultMock.isFailed()).thenReturn(false);
        when(resultMock.getResponse()).thenReturn(responseMock);
        when(responseMock.getStatus()).thenReturn(200);

        // when
        messageSender.sendMessage(SOME_MESSAGE, future);

        // then
        ArgumentCaptor<Adapter> adapterCaptor = ArgumentCaptor.forClass(Adapter.class);
        verify(requestMock).send(adapterCaptor.capture());
        adapterCaptor.getValue().onComplete(resultMock);
    }

    @Test
    public void shouldReturnFalseWhenSendingFails() throws Exception {
        // given
        CompletableFuture<MessageSendingResult> future = new CompletableFuture<>();
        when(resultMock.isFailed()).thenReturn(false);
        when(resultMock.getResponse()).thenReturn(responseMock);
        when(responseMock.getStatus()).thenReturn(500);

        // when
        messageSender.sendMessage(SOME_MESSAGE, future);

        // then
        ArgumentCaptor<Adapter> adapterCaptor = ArgumentCaptor.forClass(Adapter.class);
        verify(requestMock).send(adapterCaptor.capture());
        adapterCaptor.getValue().onComplete(resultMock);
        assertFalse(future.get(1, TimeUnit.SECONDS).succeeded());
    }

    @Test
    public void shouldReleaseSemaphoreWhenSendingFails() throws Exception {
        // given
        CompletableFuture<MessageSendingResult> future = new CompletableFuture<>();
        when(resultMock.isFailed()).thenReturn(true);
        when(resultMock.getResponse()).thenReturn(responseMock);
        when(responseMock.getStatus()).thenReturn(500);

        // when
        messageSender.sendMessage(SOME_MESSAGE, future);

        // then
        ArgumentCaptor<Adapter> adapterCaptor = ArgumentCaptor.forClass(Adapter.class);
        verify(requestMock).send(adapterCaptor.capture());
        adapterCaptor.getValue().onComplete(resultMock);
    }

    private static final class SimpleEndpointAddressResolver implements EndpointAddressResolver {
        @Override
        public URI resolve(EndpointAddress address, Message message) {
            return URI.create(address.getEndpoint());
        }
    }
}
