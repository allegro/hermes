package pl.allegro.tech.hermes.consumers.config;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.springframework.context.annotation.Bean;
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
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP2_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_MONITORING;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_SIZE;

@Configuration
public class ConsumerSenderConfiguration {

    @Bean
    public MessageBatchSenderFactory httpMessageBatchSenderFactory(ConfigFactory configFactory,
                                                                   SendingResultHandlers resultHandlers) {
        return new HttpMessageBatchSenderFactory(configFactory, resultHandlers);
    }

    @Bean
    public MessageSenderFactory messageSenderFactory(ProtocolMessageSenderProvider defaultHttpMessageSenderProvider,
                                                     ProtocolMessageSenderProvider defaultJmsMessageSenderProvider) {
        MessageSenderFactory factory = new MessageSenderFactory();
        factory.addSupportedProtocol("http", defaultHttpMessageSenderProvider);
        factory.addSupportedProtocol("https", defaultHttpMessageSenderProvider);
        factory.addSupportedProtocol("jms", defaultJmsMessageSenderProvider);
        return factory;
    }

    @Bean(name = "defaultHttpMessageSenderProvider")
    public ProtocolMessageSenderProvider jettyHttpMessageSenderProvider(@Named("http-1-client") HttpClient
                                                                                httpClient,
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
                requestFactoryProvider
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
    public Http2ClientHolder http2ClientHolder(HttpClientsFactory httpClientsFactory,
                                               ConfigFactory configFactory) {
        if (!configFactory.getBooleanProperty(CONSUMER_HTTP2_ENABLED)) {
            return new Http2ClientHolder(null);
        } else {
            return new Http2ClientHolder(httpClientsFactory.createClientForHttp2());
        }
    }

    @Bean
    public HttpClientsFactory httpClientsFactory(ConfigFactory configFactory,
                                                 InstrumentedExecutorServiceFactory executorFactory,
                                                 SslContextFactoryProvider sslContextFactoryProvider) {
        return new HttpClientsFactory(configFactory, executorFactory, sslContextFactoryProvider);
    }

    @Bean
    public HttpClientsWorkloadReporter httpClientsWorkloadReporter(HermesMetrics metrics,
                                                                   @Named("http-1-client") HttpClient httpClient,
                                                                   Http2ClientHolder http2ClientHolder,
                                                                   ConfigFactory configFactory) {
        return new HttpClientsWorkloadReporter(metrics, httpClient, http2ClientHolder, configFactory);
    }

    @Bean
    public SslContextFactoryProvider sslContextFactoryProvider(Optional<SslContextFactory> sslContextFactory, ConfigFactory configFactory) {
        return new SslContextFactoryProvider(sslContextFactory.orElse(null), configFactory);
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

    @Bean
    public EndpointAddressResolver interpolatingEndpointAddressResolver(UriInterpolator interpolator) {
        return new InterpolatingEndpointAddressResolver(interpolator);
    }

    @Bean
    public FutureAsyncTimeout<MessageSendingResult> futureAsyncTimeoutFactory(ConfigFactory configFactory,
                                                                              InstrumentedExecutorServiceFactory executorFactory) {
        ScheduledExecutorService timeoutExecutorService = executorFactory.getScheduledExecutorService(
                "async-timeout",
                configFactory.getIntProperty(CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_SIZE),
                configFactory.getBooleanProperty(CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_MONITORING)
        );
        return new FutureAsyncTimeout<>(MessageSendingResult::failedResult, timeoutExecutorService);
    }
}
