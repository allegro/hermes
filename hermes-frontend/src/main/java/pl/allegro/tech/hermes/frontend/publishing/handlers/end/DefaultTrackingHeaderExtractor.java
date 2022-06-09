package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import io.undertow.server.HttpServerExchange;
import java.util.Map;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;

import static pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersToMapTransformer.toHeadersMap;

public class DefaultTrackingHeaderExtractor implements TrackingHeadersExtractor {
    private final HeadersPropagator headersPropagator;

    public DefaultTrackingHeaderExtractor(HeadersPropagator headersPropagator) {
        this.headersPropagator = headersPropagator;
    }

    @Override
    public Map<String, String> extractHeadersToLog(HttpServerExchange exchange) {
        return headersPropagator.extract(toHeadersMap(exchange.getRequestHeaders()));
    }

}