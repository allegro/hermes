package pl.allegro.tech.hermes.integration.metadata;

import com.google.common.collect.ImmutableSet;
import pl.allegro.tech.hermes.frontend.publishing.metadata.HeadersPropagator;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TraceHeadersPropagator implements HeadersPropagator {

    private static final Set<String> HEADERS = ImmutableSet.of(
            "Trace-Id", "Span-Id", "Parent-Span-Id", "Trace-Sampled", "Trace-Reported");

    @Override
    public Map<String, String> extract(Map<String, String> headers) {
        return headers.entrySet()
                .stream()
                .filter(entry -> HEADERS.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
