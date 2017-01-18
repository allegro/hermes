package pl.allegro.tech.hermes.management.domain.maintainer;

import pl.allegro.tech.hermes.api.Maintainer;

import java.util.List;

public interface HintingMaintainerSource extends MaintainerSource {

    List<Maintainer> maintainersMatching(String searchString);

}
