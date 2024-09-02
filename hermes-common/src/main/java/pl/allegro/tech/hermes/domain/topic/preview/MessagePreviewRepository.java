package pl.allegro.tech.hermes.domain.topic.preview;

import pl.allegro.tech.hermes.api.TopicName;

import java.util.List;

public interface MessagePreviewRepository {

    List<MessagePreview> loadPreview(TopicName topicName);

    void persist(TopicsMessagesPreview topicsMessagesPreview);
}
