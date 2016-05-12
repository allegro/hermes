package pl.allegro.tech.hermes.domain.topic.preview;

import pl.allegro.tech.hermes.api.TopicName;

import java.util.Collection;
import java.util.List;

public interface MessagePreviewRepository {

    List<byte[]> loadPreview(TopicName topicName);

    void persist(TopicsMessagesPreview topicsMessagesPreview);

}
