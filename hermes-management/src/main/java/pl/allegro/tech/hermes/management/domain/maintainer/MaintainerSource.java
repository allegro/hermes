package pl.allegro.tech.hermes.management.domain.maintainer;

import pl.allegro.tech.hermes.api.Maintainer;

import java.util.List;
import java.util.Optional;

public interface MaintainerSource {

    String sourceName();

    boolean exists(String maintainerId);

    List<Maintainer> maintainersMatching(String searchString);

    default Optional<Maintainer> get(String id) {
        return Optional.of(new Maintainer(id, nameForId(id)));
    }

    default String nameForId(String maintainerId) {
        return maintainerId;
    }

}
