package pl.allegro.tech.hermes.management.config.kafka;

import com.google.common.collect.ImmutableMap;

import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;

import java.util.Map;

public class KafkaNamesMappers {

    private final Map<String, KafkaNamesMapper> mappers;

    public KafkaNamesMappers(Map<String, KafkaNamesMapper> mappers) {
        this.mappers = ImmutableMap.copyOf(mappers);
    }

    public KafkaNamesMapper getMapper(String clusterName) {
        return mappers.get(clusterName);
    }
}
