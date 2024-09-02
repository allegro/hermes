package pl.allegro.tech.hermes.management.domain.subscription.validator;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.OwnerId;

public class NoOpEndpointOwnershipValidator implements EndpointOwnershipValidator {

  @Override
  public void check(OwnerId owner, EndpointAddress endpoint) {}
}
