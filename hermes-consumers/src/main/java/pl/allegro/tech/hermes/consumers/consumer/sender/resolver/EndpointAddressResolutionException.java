package pl.allegro.tech.hermes.consumers.consumer.sender.resolver;

import pl.allegro.tech.hermes.api.EndpointAddress;

@SuppressWarnings("serial")
public class EndpointAddressResolutionException extends Exception {

  private final boolean ignoreInRateCalculation;

  public EndpointAddressResolutionException(
      EndpointAddress endpointAddress, Throwable cause, boolean ignoreInRateCalculation) {
    super("Failed to resolve " + endpointAddress, cause);
    this.ignoreInRateCalculation = ignoreInRateCalculation;
  }

  public EndpointAddressResolutionException(EndpointAddress endpointAddress, Throwable cause) {
    this(endpointAddress, cause, false);
  }

  public EndpointAddressResolutionException(
      String message, Throwable cause, boolean ignoreInRateCalculation) {
    super(message, cause);
    this.ignoreInRateCalculation = ignoreInRateCalculation;
  }

  public EndpointAddressResolutionException(String message, Throwable cause) {
    this(message, cause, false);
  }

  public boolean isIgnoreInRateCalculation() {
    return ignoreInRateCalculation;
  }
}
