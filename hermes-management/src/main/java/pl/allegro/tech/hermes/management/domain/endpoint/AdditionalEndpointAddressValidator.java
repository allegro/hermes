package pl.allegro.tech.hermes.management.domain.endpoint;

import pl.allegro.tech.hermes.api.EndpointAddress;

public interface AdditionalEndpointAddressValidator {

    void check(EndpointAddress address);
}
