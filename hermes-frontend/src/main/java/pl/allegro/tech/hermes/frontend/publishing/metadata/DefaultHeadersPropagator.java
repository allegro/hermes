package pl.allegro.tech.hermes.frontend.publishing.metadata;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

public class DefaultHeadersPropagator implements HeadersPropagator {

    private final boolean propagate;
    private final Set<String> supportedHeaders;

    public DefaultHeadersPropagator(boolean enabled, String allowFilter) {
        if (enabled) {
            propagate = true;
            supportedHeaders = Arrays.stream(allowFilter.split(","))
                    .map(String::trim)
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
