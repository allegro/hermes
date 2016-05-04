package pl.allegro.tech.hermes.domain.topic.preview;

import pl.allegro.tech.hermes.api.TopicName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopicsMessagesPreview {

    private final Map<TopicName, List<byte[]>> messages = new HashMap<>();

    public void add(TopicName topicName, byte[] message) {
        List<byte[]> messageList = messages.computeIfAbsent(topicName, k -> new ArrayList<byte[]>());
        messageList.add(message);
    }

    public Collection<TopicName> topics() {
        return messages.keySet();
    }

    public List<byte[]> previewOf(TopicName topic) {
        return messages.getOrDefault(topic, new ArrayList<>());
    }
}
