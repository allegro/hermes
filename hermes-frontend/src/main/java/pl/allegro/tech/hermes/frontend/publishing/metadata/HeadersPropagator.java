package pl.allegro.tech.hermes.frontend.publishing.metadata;

import io.undertow.util.HeaderMap;
import java.util.Map;

public interface HeadersPropagator {

  Map<String, String> extract(HeaderMap headers);
}
