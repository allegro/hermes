package pl.allegro.tech.hermes.management.domain.owner;

import pl.allegro.tech.hermes.api.Owner;

import java.util.List;

public interface HintingOwnerSource extends OwnerSource {

    List<Owner> ownersMatching(String searchString);

}
