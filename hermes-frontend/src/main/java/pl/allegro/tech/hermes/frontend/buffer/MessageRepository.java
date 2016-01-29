package pl.allegro.tech.hermes.frontend.buffer;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.util.List;

public interface MessageRepository {

    void save(Message message, Topic topic);

    void delete(String messageId);

    List<BackupMessage> findAll();

    void close();
}
