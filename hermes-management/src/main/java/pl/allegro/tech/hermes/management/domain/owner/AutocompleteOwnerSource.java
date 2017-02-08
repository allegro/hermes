package pl.allegro.tech.hermes.management.domain.owner;

import pl.allegro.tech.hermes.api.Owner;

import java.util.List;

public interface AutocompleteOwnerSource extends OwnerSource {

    List<Owner> ownersMatching(String searchString);

}
