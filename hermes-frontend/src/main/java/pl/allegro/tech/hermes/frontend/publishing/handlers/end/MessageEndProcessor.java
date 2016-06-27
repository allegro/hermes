package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.metric.TopicWithMetrics;
import pl.allegro.tech.hermes.frontend.publishing.handlers.AttachmentContent;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;

public class MessageEndProcessor {

    private static final HttpString messageIdHeader = new HttpString(MESSAGE_ID.getName());

    private final Trackers trackers;
    private final BrokerListeners brokerListeners;

    @Inject
    public MessageEndProcessor(Trackers trackers, BrokerListeners brokerListeners) {
        this.trackers = trackers;
        this.brokerListeners = brokerListeners;
    }

    public void sent(HttpServerExchange exchange, AttachmentContent attachment) {
        trackers.get(attachment.getTopic()).logPublished(attachment.getMessageId(), attachment.getTopic().getName());
        sendResponse(exchange, attachment.getMessageId(), StatusCodes.CREATED);
        attachment.getTopicWithMetrics().incrementPublished();
    }

    public void delayedSent(TopicWithMetrics topicWithMetrics, Message message) {
        trackers.get(topicWithMetrics.getTopic()).logPublished(message.getId(), topicWithMetrics.getTopic().getName());
        brokerListeners.onAcknowledge(message, topicWithMetrics.getTopic());
        topicWithMetrics.incrementPublished();
    }

    public void bufferedButDelayed(HttpServerExchange exchange, Topic topic, Message message) {
        brokerListeners.onTimeout(message, topic);
        trackers.get(topic).logInflight(message.getId(), topic.getName());
        sendResponse(exchange, message.getId(), StatusCodes.ACCEPTED);
    }

    private void sendResponse(HttpServerExchange exchange, String messageId, int statusCode) {
        exchange.setStatusCode(statusCode);
        exchange.getResponseHeaders().add(messageIdHeader, messageId);
        exchange.endExchange();
    }
}
