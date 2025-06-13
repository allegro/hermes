package pl.allegro.tech.hermes.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.allegro.schema.json2avro.converter.AvroJsonConverter;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

@Configuration
public class AvroConfiguration {

  @Bean
  AvroJsonConverter avroJsonConverter() {
    return new AvroJsonConverter();
  }

  @Bean
  JsonAvroConverter jsonAvroConverter() {
    return new JsonAvroConverter();
  }
}
