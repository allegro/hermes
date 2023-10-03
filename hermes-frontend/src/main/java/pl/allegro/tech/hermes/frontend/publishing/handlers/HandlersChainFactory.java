package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.server.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageFactory;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewLog;
import pl.allegro.tech.hermes.frontend.server.auth.AuthenticationConfiguration;
import pl.allegro.tech.hermes.frontend.server.auth.AuthenticationPredicateAwareConstraintHandler;

import java.util.Optional;

public class HandlersChainFactory {

    private static final Logger logger = LoggerFactory.getLogger(HandlersChainFactory.class);

    private final TopicsCache topicsCache;
    private final MessageErrorProcessor messageErrorProcessor;
    private final MessageEndProcessor messageEndProcessor;
    private final MessageFactory messageFactory;
    private final BrokerMessageProducer brokerMessageProducer;
    private final MessagePreviewLog previewLog;
    private final boolean previewEnabled;
    private final ThroughputLimiter throughputLimiter;
    private final Optional<AuthenticationConfiguration> authenticationConfiguration;
    private final HandlersChainParameters handlersChainParameters;
    private final BrokerLatencyReporter brokerLatencyReporter;

    public HandlersChainFactory(TopicsCache topicsCache, MessageErrorProcessor messageErrorProcessor,
                                MessageEndProcessor messageEndProcessor, MessageFactory messageFactory,
                                BrokerMessageProducer brokerMessageProducer, MessagePreviewLog messagePreviewLog,
                                ThroughputLimiter throughputLimiter, Optional<AuthenticationConfiguration> authenticationConfiguration,
                                boolean messagePreviewEnabled, HandlersChainParameters handlersChainParameters,
                                BrokerLatencyReporter brokerLatencyReporter) {
        this.topicsCache = topicsCache;
        this.messageErrorProcessor = messageErrorProcessor;
        this.messageEndProcessor = messageEndProcessor;
        this.messageFactory = messageFactory;
        this.brokerMessageProducer = brokerMessageProducer;
        this.previewLog = messagePreviewLog;
        this.previewEnabled = messagePreviewEnabled;
        this.throughputLimiter = throughputLimiter;
        this.authenticationConfiguration = authenticationConfiguration;
        this.handlersChainParameters = handlersChainParameters;
        this.brokerLatencyReporter = brokerLatencyReporter;
    }

    public HttpHandler provide() {
        HttpHandler publishing = new PublishingHandler(brokerMessageProducer, messageErrorProcessor,
                messageEndProcessor, brokerLatencyReporter);
        HttpHandler messageCreateHandler = new MessageCreateHandler(publishing, messageFactory, messageErrorProcessor);
        HttpHandler timeoutHandler = new TimeoutHandler(messageEndProcessor, messageErrorProcessor);
        HttpHandler handlerAfterRead = previewEnabled ? new PreviewHandler(messageCreateHandler, previewLog) : messageCreateHandler;
        HttpHandler readHandler = new MessageReadHandler(
                handlerAfterRead,
                timeoutHandler,
                messageErrorProcessor,
                throughputLimiter,
                handlersChainParameters.isForceTopicMaxMessageSize(),
                handlersChainParameters.getIdleTimeout(),
                handlersChainParameters.getLongIdleTimeout());
        TopicHandler topicHandler = new TopicHandler(readHandler, topicsCache, messageErrorProcessor);
        boolean keepAliveHeaderEnabled = handlersChainParameters.isKeepAliveHeaderEnabled();
        HttpHandler rootPublishingHandler = keepAliveHeaderEnabled ? withKeepAliveHeaderHandler(topicHandler) : topicHandler;

        boolean authenticationEnabled = handlersChainParameters.isAuthenticationEnabled();
        return authenticationEnabled ? withAuthenticationHandlersChain(rootPublishingHandler) : rootPublishingHandler;
    }

    private HttpHandler withKeepAliveHeaderHandler(HttpHandler next) {
        return new KeepAliveHeaderHandler(next, (int) handlersChainParameters.getKeepAliveHeaderTimeout().toSeconds());
    }

    private HttpHandler withAuthenticationHandlersChain(HttpHandler next) {
        AuthenticationConfiguration authConfig = authenticationConfiguration
                .orElseThrow(() -> new IllegalStateException("AuthenticationConfiguration was not provided"));
        try {
            return createAuthenticationHandlersChain(next, authConfig);
        } catch (Exception e) {
            logger.error("Could not create authentication handlers chain", e);
            throw e;
        }
    }

    private HttpHandler createAuthenticationHandlersChain(HttpHandler next, AuthenticationConfiguration authConfig) {
        HttpHandler authenticationCallHandler = new AuthenticationCallHandler(next);
        HttpHandler constraintHandler = new AuthenticationPredicateAwareConstraintHandler(
                authenticationCallHandler, authConfig.getIsAuthenticationRequiredPredicate());

        HttpHandler mechanismsHandler = new AuthenticationMechanismsHandler(constraintHandler,
                authConfig.getAuthMechanisms());
        AuthenticationMode authenticationMode = AuthenticationMode.valueOf(
                handlersChainParameters.getAuthenticationMode().toUpperCase()
        );

        return new SecurityInitialHandler(authenticationMode, authConfig.getIdentityManager(), mechanismsHandler);
    }
}
