package pl.allegro.tech.hermes.test.helper.kafka;

import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;

public class IntegrationTestKafkaNamesMapperFactory {

  private final String namespace;

  public IntegrationTestKafkaNamesMapperFactory(String namespace) {
    this.namespace = namespace;
  }

  public KafkaNamesMapper create() {
    return new JsonToAvroMigrationKafkaNamesMapper(namespace, "_");
  }
}
