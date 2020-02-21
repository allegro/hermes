package pl.allegro.tech.hermes.consumers.consumer.filtering;

import java.util.Optional;

public enum MatchingStrategy {
    ALL,
    ANY;

    public static MatchingStrategy fromString(String value, MatchingStrategy defaultValue) {
        try {
            return Optional.ofNullable(value)
                .map(String::toUpperCase)
                .map(MatchingStrategy::valueOf)
                .orElse(defaultValue);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}
