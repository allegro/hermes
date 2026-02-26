package pl.allegro.tech.hermes.frontend.buffer;

import java.util.List;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

/**
 * @deprecated This feature is deprecated and will be removed in a future version.
 */
@Deprecated
public interface MessageRepository {

  void save(Message message, Topic topic);

  void delete(String messageId);

  List<BackupMessage> findAll();

  void close();
}
