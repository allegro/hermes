package pl.allegro.tech.hermes.domain.topic.preview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pl.allegro.tech.hermes.api.TopicName;

public class TopicsMessagesPreview {

  private final Map<TopicName, List<MessagePreview>> messages = new HashMap<>();

  public void add(TopicName topicName, MessagePreview message) {
    List<MessagePreview> messageList = messages.computeIfAbsent(topicName, k -> new ArrayList<>());
    messageList.add(message);
  }

  public Collection<TopicName> topics() {
    return messages.keySet();
  }

  public List<MessagePreview> previewOf(TopicName topic) {
    return messages.getOrDefault(topic, new ArrayList<>());
  }
}
