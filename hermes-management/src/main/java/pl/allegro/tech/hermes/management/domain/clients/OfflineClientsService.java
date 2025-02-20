package pl.allegro.tech.hermes.management.domain.clients;

import pl.allegro.tech.hermes.api.TopicName;

public interface OfflineClientsService {
  String getIframeSource(TopicName topic);
}
