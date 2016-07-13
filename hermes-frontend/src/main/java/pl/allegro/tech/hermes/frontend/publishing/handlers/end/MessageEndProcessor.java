package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
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
        sendResponse(exchange, attachment, StatusCodes.CREATED);
        attachment.getCachedTopic().incrementPublished();
    }

    public void delayedSent(CachedTopic cachedTopic, Message message) {
        trackers.get(cachedTopic.getTopic()).logPublished(message.getId(), cachedTopic.getTopic().getName());
        brokerListeners.onAcknowledge(message, cachedTopic.getTopic());
        cachedTopic.incrementPublished();
    }

    public void bufferedButDelayedProcessing(HttpServerExchange exchange, AttachmentContent attachment) {
        bufferedButDelayed(exchange, attachment);
        attachment.getCachedTopic().markDelayedProcessing();
    }

    public void bufferedButDelayed(HttpServerExchange exchange, AttachmentContent attachment) {
        Topic topic = attachment.getTopic();
        brokerListeners.onTimeout(attachment.getMessage(), topic);
        trackers.get(topic).logInflight(attachment.getMessageId(), topic.getName());
        sendResponse(exchange, attachment, StatusCodes.ACCEPTED);
    }

    private void sendResponse(HttpServerExchange exchange, AttachmentContent attachment, int statusCode) {
        exchange.setStatusCode(statusCode);
        exchange.getResponseHeaders().add(messageIdHeader, attachment.getMessageId());
        attachment.setResponseReady();
        exchange.endExchange();
    }
}
