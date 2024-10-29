package pl.allegro.tech.hermes.frontend.publishing.message;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.MediaType;
import org.apache.avro.Schema;
import org.junit.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;

public class MessageContentTypeEnforcerTest {

  private final MessageContentTypeEnforcer enforcer = new MessageContentTypeEnforcer();

  private final Topic topic =
      TopicBuilder.topic("test.Topic").withContentType(ContentType.AVRO).build();
  private final AvroUser avroMessage = new AvroUser("Bob", 30, "black");
  private final CompiledSchema<Schema> schema = CompiledSchema.of(avroMessage.getSchema(), 1, 0);
  private final CompiledSchema<Schema> testSchema =
      CompiledSchema.of(AvroUserSchemaLoader.load(), 1, 0);

  @Test
  public void shouldConvertToAvroWhenReceivedJSONOnAvroTopic() {
    // when
    byte[] enforcedMessage =
        enforcer.enforceAvro(
            "application/json", avroMessage.asJson().getBytes(), schema.getSchema(), topic);

    // then
    assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
  }

  @Test
  public void sh1ouldConvertToAvroWhenReceivedJSONOnAvroTopic() {
    // when
    byte[] enforcedMessage =
        enforcer.enforceAvro(
            "application/json", avroMessage.asJson().getBytes(), testSchema.getSchema(), topic);

    // then
    assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
  }

  @Test
  public void shouldConvertToAvroWhenReceivedAvroJSONOnAvroTopic() {
    // when
    byte[] enforcedMessage =
        enforcer.enforceAvro(
            "avro/json", avroMessage.asAvroEncodedJson().getBytes(), schema.getSchema(), topic);

    // then
    assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
  }

  @Test
  public void shouldStringContentTypeOfAdditionalOptionsWhenInterpretingIt() {
    // when
    byte[] enforcedMessage =
        enforcer.enforceAvro(
            "application/json;encoding=utf-8",
            avroMessage.asJson().getBytes(),
            schema.getSchema(),
            topic);

    // then
    assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
  }

  @Test
  public void shouldNotConvertWhenReceivingAvroOnAvroTopic() {
    // when
    byte[] enforcedMessage =
        enforcer.enforceAvro("avro/binary", avroMessage.asBytes(), schema.getSchema(), topic);

    // then
    assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
  }

  @Test
  public void shouldBeCaseInsensitiveForPayloadContentType() {
    // when
    byte[] enforcedMessage =
        enforcer.enforceAvro("AVRO/Binary", avroMessage.asBytes(), schema.getSchema(), topic);

    // then
    assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
  }

  @Test(expected = UnsupportedContentTypeException.class)
  public void shouldThrowUnsupportedContentTypeExceptionWhenReceivedWrongContentType() {
    // when
    enforcer.enforceAvro(MediaType.TEXT_PLAIN, avroMessage.asBytes(), schema.getSchema(), topic);
  }
}
