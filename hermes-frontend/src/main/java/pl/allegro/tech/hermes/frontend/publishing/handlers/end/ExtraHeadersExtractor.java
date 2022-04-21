package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;

import javax.inject.Inject;
import java.util.Map;

import static pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersToMapTransformer.toHeadersMap;

public class ExtraHeadersExtractor {
    private final HeadersPropagator headersPropagator;

    @Inject
    public ExtraHeadersExtractor(HeadersPropagator headersPropagator) {
        this.headersPropagator = headersPropagator;
    }

    public Map<String, String> extractHeadersToLog(HttpServerExchange exchange) {
        return headersPropagator.extractHeadersToLog(toHeadersMap(exchange.getRequestHeaders()));
    }
}
