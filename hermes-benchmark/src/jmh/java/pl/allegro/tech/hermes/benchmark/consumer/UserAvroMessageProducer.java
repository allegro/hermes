package pl.allegro.tech.hermes.benchmark.consumer;

import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

public interface UserAvroMessageProducer {
  AvroUser produceUser(int id);

  static UserAvroMessageProducer defaultProducer() {
    return id -> new AvroUser("Robert", id, "blue");
  }
}
