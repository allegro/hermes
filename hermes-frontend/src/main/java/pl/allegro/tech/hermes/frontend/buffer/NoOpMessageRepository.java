package pl.allegro.tech.hermes.frontend.buffer;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.util.Collections;
import java.util.List;

public class NoOpMessageRepository implements MessageRepository {
    @Override
    public void save(Message message, Topic topic) {

    }

    @Override
    public void delete(String messageId) {

    }

    @Override
    public List<BackupMessage> findAll() {
        return Collections.emptyList();
    }

    @Override
    public void close() {

    }
}
