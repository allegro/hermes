package pl.allegro.tech.hermes.domain.topic.preview;

import java.util.List;
import pl.allegro.tech.hermes.api.TopicName;

public interface MessagePreviewRepository {

  List<MessagePreview> loadPreview(TopicName topicName);

  void persist(TopicsMessagesPreview topicsMessagesPreview);
}
