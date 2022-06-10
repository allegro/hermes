package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import java.util.Map;

public interface TrackingHeadersPropagator {
    Map<String, String> extractHeadersToLog(Map<String, String> headers);
}
