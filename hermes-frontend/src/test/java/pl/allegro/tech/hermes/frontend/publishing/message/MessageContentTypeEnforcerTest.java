package pl.allegro.tech.hermes.frontend.publishing.message;

import org.apache.avro.Schema;
import org.junit.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageContentTypeEnforcerTest {

    private MessageContentTypeEnforcer enforcer = new MessageContentTypeEnforcer();

    private Topic topic = TopicBuilder.topic("test.Topic").withContentType(ContentType.AVRO).build();
    private AvroUser avroMessage = new AvroUser("Bob", 30, "black");
    private CompiledSchema<Schema> schema = CompiledSchema.of(avroMessage.getSchema(), 1, 0);
    private CompiledSchema<Schema> testSchema = CompiledSchema.of(AvroUserSchemaLoader.load(), 1, 0);

    @Test
    public void shouldConvertToAvroWhenReceivedJSONOnAvroTopic() throws IOException {
        // when
        byte[] enforcedMessage = enforcer.enforceAvro("application/json", avroMessage.asJson().getBytes(), schema.getSchema(), topic);

        // then
        assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
    }

    @Test
    public void sh1ouldConvertToAvroWhenReceivedJSONOnAvroTopic() throws IOException {
        // when
        byte[] enforcedMessage = enforcer.enforceAvro("application/json", avroMessage.asJson().getBytes(), testSchema.getSchema(), topic);

        // then
        assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
    }


    @Test
    public void shouldConvertToAvroWhenReceivedAvroJSONOnAvroTopic() throws IOException {
        // when
        byte[] enforcedMessage = enforcer.enforceAvro("avro/json", avroMessage.asAvroEncodedJson().getBytes(), schema.getSchema(), topic);

        // then
        assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
    }

    @Test
    public void shouldStringContentTypeOfAdditionalOptionsWhenInterpretingIt() throws IOException {
        // when
        byte[] enforcedMessage = enforcer.enforceAvro("application/json;encoding=utf-8", avroMessage.asJson().getBytes(), schema.getSchema(), topic);

        // then
        assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
    }

    @Test
    public void shouldNotConvertWhenReceivingAvroOnAvroTopic() throws IOException {
        // when
        byte[] enforcedMessage = enforcer.enforceAvro("avro/binary", avroMessage.asBytes(), schema.getSchema(), topic);

        // then
        assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
    }

    @Test
    public void shouldBeCaseInsensitiveForPayloadContentType() throws IOException {
        // when
        byte[] enforcedMessage = enforcer.enforceAvro("AVRO/Binary", avroMessage.asBytes(), schema.getSchema(), topic);

        // then
        assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
    }

    @Test(expected = UnsupportedContentTypeException.class)
    public void shouldThrowUnsupportedContentTypeExceptionWhenReceivedWrongContentType() throws IOException {
        // when
        enforcer.enforceAvro(MediaType.TEXT_PLAIN, avroMessage.asBytes(), schema.getSchema(), topic);
    }

}
