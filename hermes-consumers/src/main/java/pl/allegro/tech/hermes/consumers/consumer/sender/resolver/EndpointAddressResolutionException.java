package pl.allegro.tech.hermes.consumers.consumer.sender.resolver;

import pl.allegro.tech.hermes.api.EndpointAddress;

@SuppressWarnings("serial")
public class EndpointAddressResolutionException extends Exception {

    private final boolean ignoreInRateCalculation;

    public EndpointAddressResolutionException(EndpointAddress endpointAddress, Throwable cause, boolean ignoreInRateCalculation) {
        super("Failed to resolve " + endpointAddress, cause);
        this.ignoreInRateCalculation = ignoreInRateCalculation;
    }

    public EndpointAddressResolutionException(EndpointAddress endpointAddress, Throwable cause) {
        super("Failed to resolve " + endpointAddress, cause);
        this.ignoreInRateCalculation = true;
    }

    public EndpointAddressResolutionException(String message, Throwable cause) {
        super(message, cause);
        this.ignoreInRateCalculation = true;
    }

    public boolean isIgnoreInRateCalculation() {
        return ignoreInRateCalculation;
    }
}
