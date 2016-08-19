package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageIdGenerator;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

import java.util.Optional;
import java.util.function.Consumer;

import static pl.allegro.tech.hermes.api.ErrorCode.GROUP_NOT_EXISTS;
import static pl.allegro.tech.hermes.api.ErrorCode.TOPIC_BLACKLISTED;
import static pl.allegro.tech.hermes.api.ErrorCode.TOPIC_NOT_EXISTS;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;

class TopicHandler implements HttpHandler {

    private static final String UNKNOWN_TOPIC_NAME = "unknown";

    private final HttpHandler next;
    private final TopicsCache topicsCache;
    private final MessageErrorProcessor messageErrorProcessor;

    TopicHandler(HttpHandler next, TopicsCache topicsCache, MessageErrorProcessor messageErrorProcessor) {
        this.next = next;
        this.topicsCache = topicsCache;
        this.messageErrorProcessor = messageErrorProcessor;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            // switch to worker thread
            exchange.dispatch(this);
            return;
        }

        String messageId = MessageIdGenerator.generate();

        onTopicPresent(exchange, messageId, cachedTopic -> {
            exchange.addExchangeCompleteListener(new ExchangeMetrics(cachedTopic));
            exchange.putAttachment(AttachmentContent.KEY, new AttachmentContent(cachedTopic, new MessageState(), messageId));
            try {
                next.handleRequest(exchange);
            } catch (Exception e) {
                messageErrorProcessor.sendAndLog(exchange, cachedTopic.getTopic(), messageId, e);
            }
        });
    }

    private void onTopicPresent(HttpServerExchange exchange, String messageId, Consumer<CachedTopic> consumer) {
        String qualifiedTopicName = exchange.getQueryParameters().get("qualifiedTopicName").getFirst();
        try {
            Optional<CachedTopic> topic = topicsCache.getTopic(qualifiedTopicName);
            if (topic.isPresent()) {
                if (!topicsCache.isBlacklisted(qualifiedTopicName)) {
                    consumer.accept(topic.get());
                } else {
                    topicBlacklisted(exchange, qualifiedTopicName, messageId);
                }
            } else {
                nonExistentTopic(exchange, qualifiedTopicName, messageId);
            }
        } catch (IllegalArgumentException exception) {
            missingTopicGroup(exchange, qualifiedTopicName, messageId);
        }
    }

    private void missingTopicGroup(HttpServerExchange exchange, String qualifiedTopicName, String messageId) {
        messageErrorProcessor.sendQuietly(
                exchange,
                error("Missing valid topic group in path. Found " + qualifiedTopicName, GROUP_NOT_EXISTS),
                messageId,
                UNKNOWN_TOPIC_NAME);
    }

    private void nonExistentTopic(HttpServerExchange exchange, String qualifiedTopicName, String messageId) {
        messageErrorProcessor.sendQuietly(
                exchange,
                error("Topic not found: " + qualifiedTopicName, TOPIC_NOT_EXISTS),
                messageId,
                UNKNOWN_TOPIC_NAME);
    }

    private void topicBlacklisted(HttpServerExchange exchange, String qualifiedTopicName, String messageId) {
        messageErrorProcessor.sendQuietly(exchange, error("Topic blacklisted: " + qualifiedTopicName, TOPIC_BLACKLISTED),  messageId, qualifiedTopicName);
    }
}
