package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import io.undertow.server.HttpServerExchange;

import java.util.Map;

public interface TrackingHeadersExtractor {
    Map<String, String> extractHeadersToLog(HttpServerExchange exchange);
}
