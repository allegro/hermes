package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import io.undertow.util.HeaderMap;
import java.util.Map;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersToMapTransformer;

public interface TrackingHeadersExtractor {
    default Map<String, String> extractHeadersToLog(HeaderMap headers) {
        return HeadersToMapTransformer.toHeadersMap(headers);
    }
}
