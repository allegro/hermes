package pl.allegro.tech.hermes.management.domain.owner.validator;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.OwnerId;

public interface EndpointOwnershipValidator {

    void check(OwnerId owner, EndpointAddress endpoint);
}
