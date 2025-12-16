package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.google.bigquery.avro.thread-pool")
public class GoogleBigQueryAvroThreadPoolProperties extends GoogleBigQueryThreadPoolProperties {}
