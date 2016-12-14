package pl.allegro.tech.hermes.management.domain.maintainer;

import java.util.List;

public interface MaintainerSource {

    String sourceName();

    String nameForId(String maintainerId);

    List<String> maintainersMatching(String searchString);

}
