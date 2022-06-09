package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import java.util.Map;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;


public class DefaultTrackingHeaderExtractor implements TrackingHeadersExtractor {
    private final HeadersPropagator headersPropagator;

    public DefaultTrackingHeaderExtractor(HeadersPropagator headersPropagator) {
        this.headersPropagator = headersPropagator;
    }

    @Override
    public Map<String, String> extractHeadersToLog(Map<String, String> headers) {
        return headersPropagator.extract(headers);
    }

}