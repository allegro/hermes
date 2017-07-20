package pl.allegro.tech.hermes.management.domain.readers;

import pl.allegro.tech.hermes.api.TopicName;

import java.util.List;

public interface OfflineReadersService {

    List<OfflineReader> find(TopicName topic);
}
