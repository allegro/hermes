package pl.allegro.tech.hermes.management.domain.clients;

import java.util.List;
import pl.allegro.tech.hermes.api.TopicName;

public interface AllTopicClientsService {

  List<String> getAllClientsByTopic(TopicName topicName);
}
