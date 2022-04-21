package pl.allegro.tech.hermes.frontend.publishing.metadata;


import java.util.Map;

public interface HeadersPropagator {

    Map<String, String> extract(Map<String, String> headers);

    default Map<String, String> extractHeadersToLog(Map<String, String> headers) {
        return extract(headers);
    }
}
