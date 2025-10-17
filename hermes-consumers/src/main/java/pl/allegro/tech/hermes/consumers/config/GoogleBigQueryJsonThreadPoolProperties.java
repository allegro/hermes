package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.google.bigquery.json.thread-pool")
public class GoogleBigQueryJsonThreadPoolProperties extends GoogleBigQueryThreadPoolProperties {}
