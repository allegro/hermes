package pl.allegro.tech.hermes.frontend.publishing.preview;

import com.google.common.util.concurrent.AtomicLongMap;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview;
import pl.allegro.tech.hermes.domain.topic.preview.TopicsMessagesPreview;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MessagePreviewLog {

    private final MessagePreviewFactory messagePreviewFactory;

    private final int previewSizePerTopic;

    private final AtomicLongMap<TopicName> limiter = AtomicLongMap.create();

    private final ConcurrentLinkedDeque<MessagePreviewSnapshot> messages = new ConcurrentLinkedDeque<>();

    @Inject
    public MessagePreviewLog(MessagePreviewFactory messagePreviewFactory, ConfigFactory configFactory) {
        this(messagePreviewFactory, configFactory.getIntProperty(Configs.FRONTEND_MESSAGE_PREVIEW_SIZE));
    }

    MessagePreviewLog(MessagePreviewFactory messagePreviewFactory, int previewSizePerTopic) {
        this.messagePreviewFactory = messagePreviewFactory;
        this.previewSizePerTopic = previewSizePerTopic;
    }

    public void add(Topic topic, Message message) {
        long counter = limiter.getAndIncrement(topic.getName());
        if (counter < previewSizePerTopic) {
            messages.add(new MessagePreviewSnapshot(topic.getName(), messagePreviewFactory.create(message, topic.isSchemaIdAwareSerializationEnabled())));
        }
    }

    public TopicsMessagesPreview snapshotAndClean() {
        List<MessagePreviewSnapshot> snapshot = new ArrayList<>(messages);
        messages.clear();
        limiter.clear();

        TopicsMessagesPreview preview = new TopicsMessagesPreview();
        for (MessagePreviewSnapshot message : snapshot) {
            preview.add(message.topicName, message.content);
        }
        return preview;
    }

    private static class MessagePreviewSnapshot {

        final TopicName topicName;

        final MessagePreview content;

        MessagePreviewSnapshot(TopicName topicName, MessagePreview content) {
            this.topicName = topicName;
            this.content = content;
        }
    }
}