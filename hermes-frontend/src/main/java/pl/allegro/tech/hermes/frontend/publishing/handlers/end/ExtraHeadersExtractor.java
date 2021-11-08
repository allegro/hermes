package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;

import javax.inject.Inject;
import java.util.Map;

import static pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersToMapTransformer.toHeadersMap;

class ExtraHeadersExtractor {
    private final HeadersPropagator headersPropagator;

    @Inject
    public ExtraHeadersExtractor(HeadersPropagator headersPropagator) {
        this.headersPropagator = headersPropagator;
    }

    public Map<String, String> extractExtraRequestHeaders(HttpServerExchange exchange) {
        return headersPropagator.extract(toHeadersMap(exchange.getRequestHeaders()));
    }
}
