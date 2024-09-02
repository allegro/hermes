package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.Topic;

public interface CreatorRights {

  boolean allowedToManage(Topic topic);
}
