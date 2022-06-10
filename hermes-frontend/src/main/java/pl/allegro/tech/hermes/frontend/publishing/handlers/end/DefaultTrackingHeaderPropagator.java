package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import java.util.Map;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;


public class DefaultTrackingHeaderPropagator implements TrackingHeadersPropagator {
    private final HeadersPropagator headersPropagator;

    public DefaultTrackingHeaderPropagator(HeadersPropagator headersPropagator) {
        this.headersPropagator = headersPropagator;
    }

    @Override
    public Map<String, String> extractHeadersToLog(Map<String, String> headers) {
        return headersPropagator.extract(headers);
    }

}