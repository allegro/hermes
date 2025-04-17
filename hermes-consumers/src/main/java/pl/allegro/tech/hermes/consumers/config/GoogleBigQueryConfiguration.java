package pl.allegro.tech.hermes.consumers.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import com.google.cloud.bigquery.storage.v1.stub.BigQueryWriteStubSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.*;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.*;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonDataWriterPool;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonMessageTransformer;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonStreamWriterFactory;


import java.io.IOException;

@Configuration
public class GoogleBigQueryConfiguration {

    @Bean
    public BigQueryWriteSettings bigQueryWriteSettings(CredentialsProvider credentialsProvider) throws IOException {
        return BigQueryWriteSettings.create(BigQueryWriteStubSettings
                .newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build());
    }

    @Bean
    public GoogleBigQuerySenderTargetResolver bigQuerySenderTargetResolver() {
        return new GoogleBigQuerySenderTargetResolver();
    }

    @Bean
    public GoogleBigQueryJsonMessageTransformer jsonBigQueryMessageTransformer() {
        return new GoogleBigQueryJsonMessageTransformer();
    }

    @Bean
    public GoogleBigQueryAvroMessageTransformer avroGoogleBigQueryMessageTransformer() {
        return new GoogleBigQueryAvroMessageTransformer();
    }

    @Bean
    public GoogleBigQueryJsonDataWriterPool jsonDataWriterPool(GoogleBigQueryJsonStreamWriterFactory factory) {
        return new GoogleBigQueryJsonDataWriterPool(factory);
    }

    @Bean
    public GoogleBigQueryAvroDataWriterPool avroDataWriterPool(GoogleBigQueryAvroStreamWriterFactory factory) {
        return new GoogleBigQueryAvroDataWriterPool(factory);
    }

    @Bean
    public GoogleBigQueryAvroToProtoConverter avroToProtoConverter() {
        return new GoogleBigQueryAvroToProtoConverter();
    }
}
