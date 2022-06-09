package pl.allegro.tech.hermes.frontend.publishing.metadata;


import java.util.Map;

public interface HeadersPropagator {

    Map<String, String> extract(Map<String, String> headers);
}
