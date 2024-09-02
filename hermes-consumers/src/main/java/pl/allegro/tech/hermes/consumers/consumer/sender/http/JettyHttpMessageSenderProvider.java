package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMode;
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleRecipientMessageSenderAdapter;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProviderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.AuthHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HermesHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.Http1HeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.Http2HeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

public class JettyHttpMessageSenderProvider implements ProtocolMessageSenderProvider {

  private static final Logger logger =
      LoggerFactory.getLogger(JettyHttpMessageSenderProvider.class);

  private static final HttpHeadersProvider http1HeadersProvider = new Http1HeadersProvider();
  private static final HttpHeadersProvider http2HeadersProvider = new Http2HeadersProvider();

  private final HttpClient httpClient;
  private final Http2ClientHolder http2ClientHolder;
  private final EndpointAddressResolver endpointAddressResolver;
  private final MetadataAppender<Request> metadataAppender;
  private final HttpAuthorizationProviderFactory authorizationProviderFactory;
  private final HttpHeadersProvidersFactory httpHeadersProviderFactory;
  private final SendingResultHandlers sendingResultHandlers;
  private final HttpRequestFactoryProvider requestFactoryProvider;
  private final Set<String> supportedProtocols;

  public JettyHttpMessageSenderProvider(
      HttpClient httpClient,
      Http2ClientHolder http2ClientHolder,
      EndpointAddressResolver endpointAddressResolver,
      MetadataAppender<Request> metadataAppender,
      HttpAuthorizationProviderFactory authorizationProviderFactory,
      HttpHeadersProvidersFactory httpHeadersProviderFactory,
      SendingResultHandlers sendingResultHandlers,
      HttpRequestFactoryProvider requestFactoryProvider,
      Set<String> supportedProtocols) {
    this.httpClient = httpClient;
    this.http2ClientHolder = http2ClientHolder;
    this.endpointAddressResolver = endpointAddressResolver;
    this.metadataAppender = metadataAppender;
    this.authorizationProviderFactory = authorizationProviderFactory;
    this.httpHeadersProviderFactory = httpHeadersProviderFactory;
    this.sendingResultHandlers = sendingResultHandlers;
    this.requestFactoryProvider = requestFactoryProvider;
    this.supportedProtocols = supportedProtocols;
  }

  @Override
  public MessageSender create(
      Subscription subscription, ResilientMessageSender resilientMessageSender) {
    EndpointAddress endpoint = subscription.getEndpoint();
    EndpointAddressResolverMetadata endpointAddressResolverMetadata =
        subscription.getEndpointAddressResolverMetadata();
    ResolvableEndpointAddress resolvableEndpoint =
        new ResolvableEndpointAddress(
            endpoint, endpointAddressResolver, endpointAddressResolverMetadata);
    HttpRequestFactory requestFactory =
        requestFactoryProvider.provideRequestFactory(
            subscription, getHttpClient(subscription), metadataAppender);

    if (subscription.getMode() == SubscriptionMode.BROADCAST) {
      return new JettyBroadCastMessageSender(
          requestFactory,
          resolvableEndpoint,
          getHttpRequestHeadersProvider(subscription),
          sendingResultHandlers,
          resilientMessageSender);
    } else {
      JettyMessageSender jettyMessageSender =
          new JettyMessageSender(
              requestFactory,
              resolvableEndpoint,
              getHttpRequestHeadersProvider(subscription),
              sendingResultHandlers);
      return new SingleRecipientMessageSenderAdapter(jettyMessageSender, resilientMessageSender);
    }
  }

  @Override
  public Set<String> getSupportedProtocols() {
    return supportedProtocols;
  }

  private HttpHeadersProvider getHttpRequestHeadersProvider(Subscription subscription) {
    AuthHeadersProvider authProvider = getAuthHeadersProvider(subscription);
    Collection<HttpHeadersProvider> additionalProviders = httpHeadersProviderFactory.createAll();
    Collection<HttpHeadersProvider> providers =
        ImmutableSet.<HttpHeadersProvider>builder()
            .addAll(additionalProviders)
            .add(authProvider)
            .build();

    return new HermesHeadersProvider(providers);
  }

  private AuthHeadersProvider getAuthHeadersProvider(Subscription subscription) {
    Optional<HttpAuthorizationProvider> authorizationProvider =
        authorizationProviderFactory.create(subscription);
    HttpHeadersProvider httpHeadersProvider =
        subscription.isHttp2Enabled() ? http2HeadersProvider : http1HeadersProvider;

    return new AuthHeadersProvider(httpHeadersProvider, authorizationProvider.orElse(null));
  }

  private HttpClient getHttpClient(Subscription subscription) {
    if (subscription.isHttp2Enabled()) {
      return tryToGetHttp2Client(subscription);
    } else {
      logger.info("Using http/1.1 for {}.", subscription.getQualifiedName());
      return httpClient;
    }
  }

  private HttpClient tryToGetHttp2Client(Subscription subscription) {
    if (http2ClientHolder.getHttp2Client().isPresent()) {
      logger.info("Using http/2 for {}.", subscription.getQualifiedName());
      return http2ClientHolder.getHttp2Client().get();
    } else {
      logger.info(
          "Using http/1.1 for {}. Http/2 delivery is not enabled on this server.",
          subscription.getQualifiedName());
      return httpClient;
    }
  }

  @Override
  public void start() throws Exception {
    startClient(httpClient);
    http2ClientHolder.getHttp2Client().ifPresent(this::startClient);
  }

  private void startClient(HttpClient client) {
    if (client.isStopped()) {
      try {
        client.start();
      } catch (Exception ex) {
        logger.error("Could not start http client.", ex);
      }
    }
  }

  @Override
  public void stop() throws Exception {
    stopClient(httpClient);
    http2ClientHolder.getHttp2Client().ifPresent(this::stopClient);
  }

  private void stopClient(HttpClient client) {
    if (client.isRunning()) {
      try {
        client.stop();
      } catch (Exception ex) {
        logger.error("Could not stop http client", ex);
      }
    }
  }
}
