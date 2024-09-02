package pl.allegro.tech.hermes.consumers.config;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.ImmutableSet;
import jakarta.inject.Named;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import javax.jms.Message;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.UriInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessTokens;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubMessageTransformerCreator;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubSenderTargetResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.BatchHttpRequestFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.DefaultBatchHttpRequestFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.DefaultHttpMetadataAppender;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.DefaultHttpRequestFactoryProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.DefaultSendingResultHandlers;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.EmptyHttpHeadersProvidersFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.Http1ClientParameters;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.Http2ClientHolder;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsWorkloadReporter;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpHeadersProvidersFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpMessageBatchSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpRequestFactoryProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.JettyHttpMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.SendingResultHandlers;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.SslContextFactoryProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProviderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.jms.JmsHornetQMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.jms.JmsMetadataAppender;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.InterpolatingEndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

@Configuration
@EnableConfigurationProperties({
  SslContextProperties.class,
  HttpClientsMonitoringProperties.class,
  SenderAsyncTimeoutProperties.class,
  BatchProperties.class
})
public class ConsumerSenderConfiguration {

  @Bean(name = "http1-serial-client-parameters")
  @ConfigurationProperties(prefix = "consumer.http-client.serial.http1")
  public Http1ClientProperties http1SerialClientProperties() {
    return new Http1ClientProperties();
  }

  @Bean(name = "http1-serial-client")
  public HttpClient http1SerialClient(
      HttpClientsFactory httpClientsFactory,
      @Named("http1-serial-client-parameters") Http1ClientParameters http1ClientParameters) {
    return httpClientsFactory.createClientForHttp1(
        "jetty-http1-serial-client", http1ClientParameters);
  }

  @Bean(name = "http2-serial-client-parameters")
  @ConfigurationProperties(prefix = "consumer.http-client.serial.http2")
  public Http2ClientProperties http2SerialClientProperties() {
    return new Http2ClientProperties();
  }

  @Bean
  public Http2ClientHolder http2ClientHolder(
      HttpClientsFactory httpClientsFactory,
      @Named("http2-serial-client-parameters") Http2ClientProperties http2ClientProperties) {
    if (!http2ClientProperties.isEnabled()) {
      return new Http2ClientHolder(null);
    } else {
      return new Http2ClientHolder(
          httpClientsFactory.createClientForHttp2(
              "jetty-http2-serial-client", http2ClientProperties));
    }
  }

  @Bean(name = "http1-batch-client-parameters")
  @ConfigurationProperties(prefix = "consumer.http-client.batch.http1")
  public Http1ClientProperties http1BatchClientProperties() {
    return new Http1ClientProperties();
  }

  @Bean(name = "http1-batch-client")
  public HttpClient http1BatchClient(
      HttpClientsFactory httpClientsFactory,
      @Named("http1-batch-client-parameters") Http1ClientParameters http1ClientParameters) {
    return httpClientsFactory.createClientForHttp1(
        "jetty-http1-batch-client", http1ClientParameters);
  }

  @Bean(name = "oauth-http-client")
  public HttpClient oauthHttpClient(
      HttpClientsFactory httpClientsFactory,
      @Named("http1-serial-client-parameters") Http1ClientParameters http1ClientParameters) {
    return httpClientsFactory.createClientForHttp1("jetty-http-oauthclient", http1ClientParameters);
  }

  @Bean(destroyMethod = "stop")
  public BatchHttpRequestFactory batchHttpRequestFactory(
      @Named("http1-batch-client") HttpClient httpClient) {
    return new DefaultBatchHttpRequestFactory(httpClient);
  }

  @Bean
  public MessageBatchSenderFactory httpMessageBatchSenderFactory(
      SendingResultHandlers resultHandlers, BatchHttpRequestFactory batchHttpRequestFactory) {
    return new HttpMessageBatchSenderFactory(resultHandlers, batchHttpRequestFactory);
  }

  @Bean(initMethod = "start")
  public HttpClientsWorkloadReporter httpClientsWorkloadReporter(
      MetricsFacade metrics,
      @Named("http1-serial-client") HttpClient http1SerialClient,
      @Named("http1-batch-client") HttpClient http1BatchClient,
      Http2ClientHolder http2ClientHolder,
      HttpClientsMonitoringProperties monitoringProperties) {
    return new HttpClientsWorkloadReporter(
        metrics,
        http1SerialClient,
        http1BatchClient,
        http2ClientHolder,
        monitoringProperties.isRequestQueueMonitoringEnabled(),
        monitoringProperties.isConnectionPoolMonitoringEnabled());
  }

  @Bean(destroyMethod = "closeProviders")
  public MessageSenderFactory messageSenderFactory(List<ProtocolMessageSenderProvider> providers) {
    return new MessageSenderFactory(providers);
  }

  @Bean(name = "defaultHttpMessageSenderProvider")
  public ProtocolMessageSenderProvider jettyHttpMessageSenderProvider(
      @Named("http1-serial-client") HttpClient httpClient,
      Http2ClientHolder http2ClientHolder,
      EndpointAddressResolver endpointAddressResolver,
      MetadataAppender<Request> metadataAppender,
      HttpAuthorizationProviderFactory authorizationProviderFactory,
      HttpHeadersProvidersFactory httpHeadersProviderFactory,
      SendingResultHandlers sendingResultHandlers,
      HttpRequestFactoryProvider requestFactoryProvider) {
    return new JettyHttpMessageSenderProvider(
        httpClient,
        http2ClientHolder,
        endpointAddressResolver,
        metadataAppender,
        authorizationProviderFactory,
        httpHeadersProviderFactory,
        sendingResultHandlers,
        requestFactoryProvider,
        ImmutableSet.of("http", "https"));
  }

  @Bean
  public MetadataAppender<Request> defaultHttpMetadataAppender() {
    return new DefaultHttpMetadataAppender();
  }

  @Bean
  public HttpRequestFactoryProvider defaultHttpRequestFactoryProvider() {
    return new DefaultHttpRequestFactoryProvider();
  }

  @Bean
  public SendingResultHandlers defaultSendingResultHandlers() {
    return new DefaultSendingResultHandlers();
  }

  @Bean
  public HttpHeadersProvidersFactory emptyHttpHeadersProvidersFactory() {
    return new EmptyHttpHeadersProvidersFactory();
  }

  @Bean
  public HttpClientsFactory httpClientsFactory(
      InstrumentedExecutorServiceFactory executorFactory,
      SslContextFactoryProvider sslContextFactoryProvider) {
    return new HttpClientsFactory(executorFactory, sslContextFactoryProvider);
  }

  @Bean
  public SslContextFactoryProvider sslContextFactoryProvider(
      Optional<SslContextFactory> sslContextFactory, SslContextProperties sslContextProperties) {
    return new SslContextFactoryProvider(sslContextFactory.orElse(null), sslContextProperties);
  }

  @Bean
  public HttpAuthorizationProviderFactory httpAuthorizationProviderFactory(
      OAuthAccessTokens accessTokens) {
    return new HttpAuthorizationProviderFactory(accessTokens);
  }

  @Bean(name = "defaultJmsMessageSenderProvider")
  public ProtocolMessageSenderProvider jmsHornetQMessageSenderProvider(
      MetadataAppender<Message> metadataAppender) {
    return new JmsHornetQMessageSenderProvider(metadataAppender);
  }

  @Bean
  public MetadataAppender<Message> jmsMetadataAppender() {
    return new JmsMetadataAppender();
  }

  @Bean(name = "defaultPubSubMessageSenderProvider")
  public ProtocolMessageSenderProvider pubSubMessageSenderProvider(
      GooglePubSubSenderTargetResolver targetResolver,
      CredentialsProvider credentialsProvider,
      ExecutorProvider executorProvider,
      RetrySettings retrySettings,
      BatchingSettings batchingSettings,
      GooglePubSubMessageTransformerCreator googlePubSubMessageTransformerCreator,
      TransportChannelProvider transportChannelProvider) {
    return new GooglePubSubMessageSenderProvider(
        targetResolver,
        credentialsProvider,
        executorProvider,
        retrySettings,
        batchingSettings,
        transportChannelProvider,
        googlePubSubMessageTransformerCreator);
  }

  @Bean
  @Conditional(OnGoogleDefaultCredentials.class)
  public CredentialsProvider applicationDefaultCredentialsProvider() throws IOException {
    return FixedCredentialsProvider.create(GoogleCredentials.getApplicationDefault());
  }

  @Bean
  @ConditionalOnMissingBean(CredentialsProvider.class)
  public CredentialsProvider noCredentialsProvider() {
    return NoCredentialsProvider.create();
  }

  @Bean
  public EndpointAddressResolver interpolatingEndpointAddressResolver(
      UriInterpolator interpolator) {
    return new InterpolatingEndpointAddressResolver(interpolator);
  }

  @Bean
  public FutureAsyncTimeout futureAsyncTimeoutFactory(
      InstrumentedExecutorServiceFactory executorFactory,
      SenderAsyncTimeoutProperties senderAsyncTimeoutProperties) {
    ScheduledExecutorService timeoutExecutorService =
        executorFactory
            .scheduledExecutorBuilder(
                "async-timeout", senderAsyncTimeoutProperties.getThreadPoolSize())
            .withMonitoringEnabled(senderAsyncTimeoutProperties.isThreadPoolMonitoringEnabled())
            .create();
    return new FutureAsyncTimeout(timeoutExecutorService);
  }
}
