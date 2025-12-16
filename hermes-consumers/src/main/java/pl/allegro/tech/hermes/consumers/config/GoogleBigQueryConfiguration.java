package pl.allegro.tech.hermes.consumers.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import com.google.cloud.bigquery.storage.v1.stub.BigQueryWriteStubSettings;
import java.io.IOException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQuerySenderTargetResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.ThreadPoolProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroDataWriterPool;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroMessageTransformer;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroStreamWriterFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroToProtoConverter;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonDataWriterPool;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonMessageTransformer;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonStreamWriterFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonWriteClientProvider;

@Configuration
@EnableConfigurationProperties({
  GoogleBigQueryJsonStreamWriterProperties.class,
  GoogleBigQueryAvroStreamWriterProperties.class,
  GoogleBigQueryJsonThreadPoolProperties.class,
  GoogleBigQueryAvroThreadPoolProperties.class,
})
public class GoogleBigQueryConfiguration {

  @Bean
  public BigQueryWriteSettings bigQueryWriteSettings(CredentialsProvider credentialsProvider)
      throws IOException {
    return BigQueryWriteSettings.create(
        BigQueryWriteStubSettings.newBuilder().setCredentialsProvider(credentialsProvider).build());
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
  public GoogleBigQueryJsonDataWriterPool jsonDataWriterPool(
      GoogleBigQueryJsonStreamWriterFactory factory) {
    return new GoogleBigQueryJsonDataWriterPool(factory);
  }

  @Bean
  public GoogleBigQueryAvroDataWriterPool avroDataWriterPool(
      GoogleBigQueryAvroStreamWriterFactory factory) {
    return new GoogleBigQueryAvroDataWriterPool(factory);
  }

  @Bean
  public GoogleBigQueryAvroToProtoConverter avroToProtoConverter() {
    return new GoogleBigQueryAvroToProtoConverter();
  }

  @Bean
  public GoogleBigQueryAvroStreamWriterFactory avroStreamWriterFactory(
      GoogleBigQueryAvroStreamWriterProperties avroStreamWriterProperties,
      GoogleBigQueryAvroThreadPoolProperties avroThreadPoolProperties,
      CredentialsProvider credentialsProvider,
      BigQueryWriteSettings writeSettings,
      GoogleBigQueryAvroToProtoConverter avroToProtoConverter)
      throws IOException {
    ThreadPoolProvider threadPoolProvider = new ThreadPoolProvider(avroThreadPoolProperties);
    return new GoogleBigQueryAvroStreamWriterFactory(
        avroStreamWriterProperties,
        credentialsProvider,
        threadPoolProvider,
        writeSettings,
        avroToProtoConverter);
  }

  @Bean
  public GoogleBigQueryJsonStreamWriterFactory jsonStreamWriterFactory(
      GoogleBigQueryJsonStreamWriterProperties jsonStreamWriterProperties,
      GoogleBigQueryAvroThreadPoolProperties jsonThreadPoolProperties,
      CredentialsProvider credentialsProvider,
      GoogleBigQueryJsonWriteClientProvider writeClientProvider)
      throws IOException {
    ThreadPoolProvider threadPoolProvider = new ThreadPoolProvider(jsonThreadPoolProperties);
    return new GoogleBigQueryJsonStreamWriterFactory(
        jsonStreamWriterProperties, credentialsProvider, threadPoolProvider, writeClientProvider);
  }

  @Bean
  public GoogleBigQueryJsonWriteClientProvider jsonWriteClientProvider(
      BigQueryWriteSettings writeSettings) {
    return new GoogleBigQueryJsonWriteClientProvider(writeSettings);
  }
}
