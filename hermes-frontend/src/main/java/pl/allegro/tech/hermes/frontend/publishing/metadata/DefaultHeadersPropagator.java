package pl.allegro.tech.hermes.frontend.publishing.metadata;

import com.google.common.collect.ImmutableMap;
import pl.allegro.tech.hermes.common.config.ConfigFactory;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_HEADER_PROPAGATION_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_HEADER_PROPAGATION_ALLOW_FILTER;

public class DefaultHeadersPropagator implements HeadersPropagator {

    private final boolean propagate;
    private final Set<String> supportedHeaders;

    @Inject
    public DefaultHeadersPropagator(ConfigFactory config) {
        if (config.getBooleanProperty(FRONTEND_HEADER_PROPAGATION_ENABLED)) {
            propagate = true;
            supportedHeaders = asList(config.getStringProperty(FRONTEND_HEADER_PROPAGATION_ALLOW_FILTER).split(",")).stream()
                    .map(v -> v.trim())
                    .filter(v -> v.length() > 0)
                    .collect(toSet());
        } else {
            propagate = false;
            supportedHeaders = emptySet();
        }
    }

    @Override
    public Map<String, String> extract(Map<String, String> headers) {
        if (propagate) {
            if (supportedHeaders.isEmpty()) {
                return ImmutableMap.copyOf(headers);
            }
            return headers.entrySet().stream()
                    .filter(e -> this.supportedHeaders.contains(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return ImmutableMap.of();
        }
    }
}
