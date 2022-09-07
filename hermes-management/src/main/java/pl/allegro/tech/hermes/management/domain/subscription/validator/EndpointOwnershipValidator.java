package pl.allegro.tech.hermes.management.domain.subscription.validator;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;

public interface EndpointOwnershipValidator {

    void check(OwnerId owner, RequestUser createdBy, EndpointAddress endpoint);
}
