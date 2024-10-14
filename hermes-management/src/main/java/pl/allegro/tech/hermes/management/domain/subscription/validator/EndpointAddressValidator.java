package pl.allegro.tech.hermes.management.domain.subscription.validator;

import pl.allegro.tech.hermes.api.EndpointAddress;

public interface EndpointAddressValidator {

  void check(EndpointAddress address);
}
