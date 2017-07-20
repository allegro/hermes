package pl.allegro.tech.hermes.management.domain.readers;

import java.util.List;

public interface OfflineReadersService {

    List<OfflineReader> find(String topic);
}
