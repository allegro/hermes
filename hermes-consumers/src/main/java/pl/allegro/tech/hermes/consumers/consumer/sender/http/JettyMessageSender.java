package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.Destination;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.Request;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpRequestData.HttpRequestDataBuilder;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpRequestHeaders;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class JettyMessageSender implements CompletableFutureAwareMessageSender {

    private final HttpRequestFactory requestFactory;
    private final ResolvableEndpointAddress addressResolver;
    private final HttpHeadersProvider requestHeadersProvider;
    private final SendingResultHandlers sendingResultHandlers;
    private final AuthorityResolver authorityResolver;
    private final HttpClient httpClient;

    public JettyMessageSender(HttpRequestFactory requestFactory,
                              ResolvableEndpointAddress addressResolver,
                              HttpHeadersProvider headersProvider,
                              SendingResultHandlers sendingResultHandlers,
                              AuthorityResolver authorityResolver,
                              HttpClient httpClient) {
        this.requestFactory = requestFactory;
        this.addressResolver = addressResolver;
        this.requestHeadersProvider = headersProvider;
        this.sendingResultHandlers = sendingResultHandlers;
        this.authorityResolver = authorityResolver;
        this.httpClient = httpClient;
    }

    @Override
    public void send(Message message, final CompletableFuture<MessageSendingResult> resultFuture) {
        try {
            final HttpRequestData requestData = new HttpRequestDataBuilder()
                    .withRawAddress(addressResolver.getRawAddress())
                    .build();

            HttpRequestHeaders headers = requestHeadersProvider.getHeaders(message, requestData);

            URI resolvedUri = addressResolver.resolveFor(message);

            Origin origin = new Origin(resolvedUri.getScheme(), resolvedUri.getHost(), resolvedUri.getPort());

            Destination destination = httpClient.resolveDestination(origin);
            URI authority = authorityResolver.resolveAuthority(resolvedUri, message, requestData);

            Request request = requestFactory.buildRequest(message, authority, headers);

            destination.send(request, sendingResultHandlers.handleSendingResultForSerial(resultFuture));

        } catch (EndpointAddressResolutionException exception) {
            resultFuture.complete(MessageSendingResult.failedResult(exception));
        }
    }

    @Override
    public void stop() {
    }
}
