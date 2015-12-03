package pl.allegro.tech.hermes.frontend.publishing.metadata;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class DefaultHeadersPropagator implements HeadersPropagator {

    @Override
    public Map<String, String> extract(Map<String, String> headers) {
        return ImmutableMap.of();
    }
}
