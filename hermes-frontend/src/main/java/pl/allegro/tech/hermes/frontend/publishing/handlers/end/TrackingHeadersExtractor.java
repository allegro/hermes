package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import java.util.Map;

public interface TrackingHeadersExtractor {
    Map<String, String> extractHeadersToLog(Map<String, String> headers);
}
