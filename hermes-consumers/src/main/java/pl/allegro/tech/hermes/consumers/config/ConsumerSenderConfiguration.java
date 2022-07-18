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
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.UriInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessTokens;
import pl.allegro.tech.hermes.consumers.consumer.sender.HttpMessageBatchSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubMessages;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubSenderTargetResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.DefaultHttpMetadataAppender;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.DefaultHttpRequestFactoryProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.DefaultSendingResultHandlers;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.EmptyHttpHeadersProvidersFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.Http2ClientHolder;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsWorkloadReporter;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpHeadersProvidersFactory;
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

import javax.inject.Named;
import javax.jms.Message;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableConfigurationProperties({
        SslContextProperties.class,
        HttpClientProperties.class,
        Http2ClientProperties.class,
        SenderAsyncTimeoutProperties.class,
        BatchProperties.class
})
public class ConsumerSenderConfiguration {

    @Bean
    public MessageBatchSenderFactory httpMessageBatchSenderFactory(SendingResultHandlers resultHandlers,
                                                                   BatchProperties batchProperties) {
        return new HttpMessageBatchSenderFactory(resultHandlers, batchProperties.getConnectionTimeout(), batchProperties.getConnectionRequestTimeout());
    }

    @Bean(destroyMethod = "closeProviders")
    public MessageSenderFactory messageSenderFactory(List<ProtocolMessageSenderProvider> providers) {
        return new MessageSenderFactory(providers);
    }

    @Bean(name = "defaultHttpMessageSenderProvider")
    public ProtocolMessageSenderProvider jettyHttpMessageSenderProvider(@Named("http-1-client") HttpClient httpClient,
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
                ImmutableSet.of("http", "https")
        );
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
    public Http2ClientHolder http2ClientHolder(HttpClientsFactory httpClientsFactory, Http2ClientProperties http2ClientProperties) {
        if (!http2ClientProperties.isEnabled()) {
            return new Http2ClientHolder(null);
        } else {
            return new Http2ClientHolder(httpClientsFactory.createClientForHttp2());
        }
    }

    @Bean
    public HttpClientsFactory httpClientsFactory(HttpClientProperties httpClientProperties,
                                                 Http2ClientProperties http2ClientProperties,
                                                 InstrumentedExecutorServiceFactory executorFactory,
                                                 SslContextFactoryProvider sslContextFactoryProvider) {
        return new HttpClientsFactory(httpClientProperties, http2ClientProperties, executorFactory, sslContextFactoryProvider);
    }

    @Bean(initMethod = "start")
    public HttpClientsWorkloadReporter httpClientsWorkloadReporter(HermesMetrics metrics,
                                                                   @Named("http-1-client") HttpClient httpClient,
                                                                   Http2ClientHolder http2ClientHolder,
                                                                   HttpClientProperties httpClientProperties) {
        return new HttpClientsWorkloadReporter(metrics, httpClient, http2ClientHolder, httpClientProperties.isRequestQueueMonitoringEnabled(), httpClientProperties.isConnectionPoolMonitoringEnabled());
    }

    @Bean
    public SslContextFactoryProvider sslContextFactoryProvider(Optional<SslContextFactory> sslContextFactory, SslContextProperties sslContextProperties) {
        return new SslContextFactoryProvider(sslContextFactory.orElse(null), sslContextProperties);
    }

    @Bean
    public HttpAuthorizationProviderFactory httpAuthorizationProviderFactory(OAuthAccessTokens accessTokens) {
        return new HttpAuthorizationProviderFactory(accessTokens);
    }

    @Bean(name = "defaultJmsMessageSenderProvider")
    public ProtocolMessageSenderProvider jmsHornetQMessageSenderProvider(ConfigFactory configFactory,
                                                                         MetadataAppender<Message> metadataAppender) {
        return new JmsHornetQMessageSenderProvider(configFactory, metadataAppender);
    }

    @Bean
    public MetadataAppender<Message> jmsMetadataAppender() {
        return new JmsMetadataAppender();
    }

    @Bean(name = "defaultPubSubMessageSenderProvider")
    public ProtocolMessageSenderProvider pubSubMessageSenderProvider(GooglePubSubSenderTargetResolver targetResolver,
                                                                     CredentialsProvider credentialsProvider,
                                                                     ExecutorProvider executorProvider,
                                                                     RetrySettings retrySettings,
                                                                     BatchingSettings batchingSettings,
                                                                     GooglePubSubMessages googlePubSubMessages,
                                                                     TransportChannelProvider transportChannelProvider) {
        return new GooglePubSubMessageSenderProvider(
                targetResolver,
                credentialsProvider,
                executorProvider,
                retrySettings,
                batchingSettings,
                transportChannelProvider,
                googlePubSubMessages
        );
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
    public EndpointAddressResolver interpolatingEndpointAddressResolver(UriInterpolator interpolator) {
        return new InterpolatingEndpointAddressResolver(interpolator);
    }

    @Bean
    public FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeoutFactory(InstrumentedExecutorServiceFactory executorFactory,
                                                                              SenderAsyncTimeoutProperties senderAsyncTimeoutProperties) {
        ScheduledExecutorService timeoutExecutorService = executorFactory.getScheduledExecutorService(
                "async-timeout",
                senderAsyncTimeoutProperties.getThreadPoolSize(),
                senderAsyncTimeoutProperties.isThreadPoolMonitoringEnabled()
        );
        return new FutureAsyncTimeout<>(MessageSendingResult::failedResult, timeoutExecutorService);
    }
}
