package pl.allegro.tech.hermes.frontend.publishing.message.preview;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.util.List;

public interface PreviewMessageLog {
    void add(byte[] messageContent, TopicName topicName);

    void persist();
}