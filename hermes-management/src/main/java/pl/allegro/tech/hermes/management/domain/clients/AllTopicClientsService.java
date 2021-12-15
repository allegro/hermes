package pl.allegro.tech.hermes.management.domain.clients;

import pl.allegro.tech.hermes.api.TopicName;

import java.util.List;

public interface AllTopicClientsService {

    List<String> getAllClientsByTopic(TopicName topicName);
}
