package pl.allegro.tech.hermes.management.domain.maintainer;

import java.util.List;

public interface MaintainerSource {

    String sourceName();

    boolean exists(String maintainerId);

    List<String> maintainersMatching(String searchString);

    default String nameForId(String maintainerId) {
        return maintainerId;
    }

}
