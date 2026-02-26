package pl.allegro.tech.hermes.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import pl.allegro.tech.hermes.test.helper.util.Ports;

class HermesMockDefineValidationTest {

  @Test
  void shouldThrowExceptionWhenTopicNameIsNotQualified() {
    // given
    int port = Ports.nextAvailable();
    HermesMock hermesMock = new HermesMock.Builder().withPort(port).build();
    hermesMock.start();

    try {
      // expect
      assertThatThrownBy(() -> hermesMock.define().jsonTopic("invalid-topic-name"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid qualified name");
    } finally {
      hermesMock.stop();
    }
  }

  @Test
  void shouldThrowExceptionWhenAvroTopicNameIsNotQualified() {
    // given
    int port = Ports.nextAvailable();
    HermesMock hermesMock = new HermesMock.Builder().withPort(port).build();
    hermesMock.start();

    try {
      // expect
      assertThatThrownBy(() -> hermesMock.define().avroTopic("invalid-topic-name"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid qualified name");
    } finally {
      hermesMock.stop();
    }
  }

  @Test
  void shouldAcceptValidQualifiedTopicName() {
    // given
    int port = Ports.nextAvailable();
    HermesMock hermesMock = new HermesMock.Builder().withPort(port).build();
    hermesMock.start();

    try {
      // when
      hermesMock.define().jsonTopic("group.topic");
      hermesMock.define().avroTopic("group.another-topic");

      // then - no exception thrown
      assertThat(hermesMock).isNotNull();
    } finally {
      hermesMock.stop();
    }
  }
}
