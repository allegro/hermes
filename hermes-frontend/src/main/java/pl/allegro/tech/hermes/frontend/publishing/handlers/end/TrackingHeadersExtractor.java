package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import io.undertow.util.HeaderMap;
import java.util.Map;

public interface TrackingHeadersExtractor {
  Map<String, String> extractHeadersToLog(HeaderMap headers);
}
