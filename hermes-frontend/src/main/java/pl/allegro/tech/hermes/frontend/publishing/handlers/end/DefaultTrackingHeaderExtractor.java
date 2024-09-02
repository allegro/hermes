package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import io.undertow.util.HeaderMap;
import java.util.Collections;
import java.util.Map;

public class DefaultTrackingHeaderExtractor implements TrackingHeadersExtractor {

  @Override
  public Map<String, String> extractHeadersToLog(HeaderMap headers) {
    return Collections.emptyMap();
  }
}
