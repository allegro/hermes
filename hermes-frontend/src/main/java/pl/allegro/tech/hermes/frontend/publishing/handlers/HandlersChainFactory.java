package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.HttpHandler;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageFactory;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewLog;

import javax.inject.Inject;

public class HandlersChainFactory implements Factory<HttpHandler> {

    private final TopicsCache topicsCache;
    private final MessageErrorProcessor messageErrorProcessor;
    private final MessageEndProcessor messageEndProcessor;
    private final ConfigFactory configFactory;
    private final MessageFactory messageFactory;
    private final BrokerMessageProducer brokerMessageProducer;
    private final MessagePreviewLog previewLog;
    private final boolean previewEnabled;

    @Inject
    public HandlersChainFactory(TopicsCache topicsCache, MessageErrorProcessor messageErrorProcessor,
                                MessageEndProcessor messageEndProcessor, ConfigFactory configFactory, MessageFactory messageFactory,
                                BrokerMessageProducer brokerMessageProducer, MessagePreviewLog messagePreviewLog) {
        this.topicsCache = topicsCache;
        this.messageErrorProcessor = messageErrorProcessor;
        this.messageEndProcessor = messageEndProcessor;
        this.configFactory = configFactory;
        this.messageFactory = messageFactory;
        this.brokerMessageProducer = brokerMessageProducer;
        this.previewLog = messagePreviewLog;
        this.previewEnabled = configFactory.getBooleanProperty(Configs.FRONTEND_MESSAGE_PREVIEW_ENABLED);
    }

    @Override
    public HttpHandler provide() {
        HttpHandler publishing = new PublishingHandler(brokerMessageProducer, messageErrorProcessor, messageEndProcessor);
        HttpHandler messageCreateHandler = new MessageCreateHandler(publishing, messageFactory, messageErrorProcessor);
        HttpHandler timeoutHandler = new TimeoutHandler(messageEndProcessor, messageErrorProcessor);
        HttpHandler handlerAfterRead = previewEnabled ? new PreviewHandler(messageCreateHandler, previewLog) : messageCreateHandler;
        HttpHandler readHandler = new MessageReadHandler(handlerAfterRead, timeoutHandler, configFactory, messageErrorProcessor);
        TopicHandler topicHandler = new TopicHandler(readHandler, topicsCache, messageErrorProcessor);

        return topicHandler;
    }

    @Override
    public void dispose(HttpHandler instance) {

    }
}
