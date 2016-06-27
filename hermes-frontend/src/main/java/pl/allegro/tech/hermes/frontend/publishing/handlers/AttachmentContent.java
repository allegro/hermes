package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.util.AttachmentKey;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.metric.TopicWithMetrics;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

public class AttachmentContent {

    public static final AttachmentKey<AttachmentContent> KEY = AttachmentKey.create(AttachmentContent.class);

    private final TopicWithMetrics topicWithMetrics;
    private final MessageState messageState;
    private final String messageId;
    private byte[] messageContent;
    private volatile TimeoutHolder timeoutHolder;
    private volatile Message message;

    public Message getMessage() {
        return message;
    }

    AttachmentContent(TopicWithMetrics topicWithMetrics, MessageState messageState, String messageId) {
        this.topicWithMetrics = topicWithMetrics;
        this.messageState = messageState;
        this.messageId = messageId;
    }

    public MessageState getMessageState() {
        return messageState;
    }

    public TimeoutHolder getTimeoutHolder() {
        return timeoutHolder;
    }

    public void removeTimeout() {
        timeoutHolder.remove();
    }

    public void setTimeoutHolder(TimeoutHolder timeoutHolder) {
        this.timeoutHolder = timeoutHolder;
    }

    public Topic getTopic() {
        return topicWithMetrics.getTopic();
    }

    public TopicWithMetrics getTopicWithMetrics() {
        return topicWithMetrics;
    }

    public void setMessageContent(byte[] messageContent) {
        this.messageContent = messageContent;
    }

    public String getMessageId() {
        return messageId;
    }

    public byte[] getMessageContent() {
        return messageContent;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

}
