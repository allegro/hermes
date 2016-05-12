package pl.allegro.tech.hermes.frontend.publishing.preview;

import com.google.common.util.concurrent.AtomicLongMap;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.topic.preview.TopicsMessagesPreview;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MessagePreviewLog {

    private final int previewSizePerTopic;

    private final AtomicLongMap<TopicName> limiter = AtomicLongMap.create();

    private final ConcurrentLinkedDeque<MessagePreview> messages = new ConcurrentLinkedDeque<>();

    @Inject
    public MessagePreviewLog(ConfigFactory configFactory) {
        this(configFactory.getIntProperty(Configs.FRONTEND_MESSAGE_PREVIEW_SIZE));
    }

    public MessagePreviewLog(int previewSizePerTopic) {
        this.previewSizePerTopic = previewSizePerTopic;
    }

    public void add(TopicName topicName, byte[] messageContent) {
        long counter = limiter.getAndIncrement(topicName);
        if (counter < previewSizePerTopic) {
            messages.add(new MessagePreview(topicName, messageContent));
        }
    }

    public TopicsMessagesPreview snapshotAndClean() {
        List<MessagePreview> snapshot = new ArrayList<>(messages);
        messages.clear();
        limiter.clear();

        TopicsMessagesPreview preview = new TopicsMessagesPreview();
        for (MessagePreview message : snapshot) {
            preview.add(message.topicName, message.content);
        }
        return preview;
    }

    private static class MessagePreview {

        final TopicName topicName;

        final byte[] content;

        MessagePreview(TopicName topicName, byte[] content) {
            this.topicName = topicName;
            this.content = content;
        }
    }
}