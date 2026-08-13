package pl.allegro.tech.hermes.api;

public record KafkaTopicConfigDiff(String key, String expected, String actual) {}
