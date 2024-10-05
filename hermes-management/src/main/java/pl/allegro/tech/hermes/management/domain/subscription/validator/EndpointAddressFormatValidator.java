package pl.allegro.tech.hermes.management.domain.subscription.validator;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.damnhandy.uri.template.UriTemplate;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pl.allegro.tech.hermes.api.EndpointAddress;

public class EndpointAddressFormatValidator implements EndpointAddressValidator {
  private final Set<String> availableProtocol = new HashSet<>();

  public EndpointAddressFormatValidator(List<String> additionalEndpointProtocols) {
    this.availableProtocol.addAll(additionalEndpointProtocols);
    this.availableProtocol.add("http");
    this.availableProtocol.add("https");
    this.availableProtocol.add("jms");
    this.availableProtocol.add("googlepubsub");
  }

  @Override
  public void check(EndpointAddress address) {
    checkIfProtocolIsValid(address);
    checkIfUriIsValid(address);
  }

  private void checkIfProtocolIsValid(EndpointAddress address) {
    if (!availableProtocol.contains(address.getProtocol())) {
      throw new EndpointValidationException("Endpoint address has invalid format");
    }
  }

  private void checkIfUriIsValid(EndpointAddress address) {
    UriTemplate template = UriTemplate.fromTemplate(address.getRawEndpoint());
    if (isInvalidHost(template)) {
      throw new EndpointValidationException(
          "Endpoint contains invalid chars in host name. Underscore is one of them.");
    }
  }

  private boolean isInvalidHost(UriTemplate template) {
    Map<String, Object> uriKeysWithEmptyValues =
        asList(template.getVariables()).stream().collect(toMap(identity(), v -> "empty"));

    // check if host is null due to bug in jdk
    // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6587184
    return URI.create(template.expand(uriKeysWithEmptyValues)).getHost() == null;
  }
}
