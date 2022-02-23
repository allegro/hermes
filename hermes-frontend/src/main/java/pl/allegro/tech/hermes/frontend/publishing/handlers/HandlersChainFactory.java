package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.server.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageFactory;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewLog;
import pl.allegro.tech.hermes.frontend.server.auth.AuthenticationConfiguration;
import pl.allegro.tech.hermes.frontend.server.auth.AuthenticationPredicateAwareConstraintHandler;

import javax.inject.Inject;

import java.util.Optional;

import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_AUTHENTICATION_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_KEEP_ALIVE_HEADER_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_KEEP_ALIVE_HEADER_TIMEOUT_SECONDS;

public class HandlersChainFactory {

    private static final Logger logger = LoggerFactory.getLogger(HandlersChainFactory.class);

    private final TopicsCache topicsCache;
    private final MessageErrorProcessor messageErrorProcessor;
    private final MessageEndProcessor messageEndProcessor;
    private final ConfigFactory configFactory;
    private final MessageFactory messageFactory;
    private final BrokerMessageProducer brokerMessageProducer;
    private final MessagePreviewLog previewLog;
    private final boolean previewEnabled;
    private final ThroughputLimiter throughputLimiter;
    private final Optional<AuthenticationConfiguration> authenticationConfiguration;

    @Inject
    public HandlersChainFactory(TopicsCache topicsCache, MessageErrorProcessor messageErrorProcessor,
                                MessageEndProcessor messageEndProcessor, ConfigFactory configFactory, MessageFactory messageFactory,
                                BrokerMessageProducer brokerMessageProducer, MessagePreviewLog messagePreviewLog,
                                ThroughputLimiter throughputLimiter, AuthenticationConfiguration authenticationConfiguration) {
        this.topicsCache = topicsCache;
        this.messageErrorProcessor = messageErrorProcessor;
        this.messageEndProcessor = messageEndProcessor;
        this.configFactory = configFactory;
        this.messageFactory = messageFactory;
        this.brokerMessageProducer = brokerMessageProducer;
        this.previewLog = messagePreviewLog;
        this.previewEnabled = configFactory.getBooleanProperty(Configs.FRONTEND_MESSAGE_PREVIEW_ENABLED);
        this.throughputLimiter = throughputLimiter;
        this.authenticationConfiguration = Optional.ofNullable(authenticationConfiguration);
    }

    public HttpHandler provide() {
        HttpHandler publishing = new PublishingHandler(brokerMessageProducer, messageErrorProcessor, messageEndProcessor);
        HttpHandler messageCreateHandler = new MessageCreateHandler(publishing, messageFactory, messageErrorProcessor);
        HttpHandler timeoutHandler = new TimeoutHandler(messageEndProcessor, messageErrorProcessor);
        HttpHandler handlerAfterRead = previewEnabled ? new PreviewHandler(messageCreateHandler, previewLog) : messageCreateHandler;
        HttpHandler readHandler = new MessageReadHandler(handlerAfterRead, timeoutHandler, configFactory,
                                                                messageErrorProcessor, throughputLimiter);
        TopicHandler topicHandler = new TopicHandler(readHandler, topicsCache, messageErrorProcessor);
        boolean keepAliveHeaderEnabled = configFactory.getBooleanProperty(FRONTEND_KEEP_ALIVE_HEADER_ENABLED);
        HttpHandler rootPublishingHandler = keepAliveHeaderEnabled ? withKeepAliveHeaderHandler(topicHandler) : topicHandler;

        boolean authenticationEnabled = configFactory.getBooleanProperty(FRONTEND_AUTHENTICATION_ENABLED);
        return authenticationEnabled ? withAuthenticationHandlersChain(rootPublishingHandler) : rootPublishingHandler;
    }

    private HttpHandler withKeepAliveHeaderHandler(HttpHandler next) {
        int keepAliveTimeout = configFactory.getIntProperty(FRONTEND_KEEP_ALIVE_HEADER_TIMEOUT_SECONDS);
        return new KeepAliveHeaderHandler(next, keepAliveTimeout);
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
                configFactory.getStringProperty(Configs.FRONTEND_AUTHENTICATION_MODE).toUpperCase());

        return new SecurityInitialHandler(authenticationMode, authConfig.getIdentityManager(), mechanismsHandler);
    }
}
