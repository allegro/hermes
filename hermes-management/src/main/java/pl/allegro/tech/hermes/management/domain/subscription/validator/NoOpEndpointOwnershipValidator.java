package pl.allegro.tech.hermes.management.domain.subscription.validator;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;

public class NoOpEndpointOwnershipValidator implements EndpointOwnershipValidator {

    @Override
    public void check(OwnerId owner, RequestUser createdBy, EndpointAddress endpoint) {

    }
}
