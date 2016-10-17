package pl.allegro.tech.hermes.frontend.publishing.message;

import org.apache.avro.Schema;
import org.junit.Test;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageContentTypeEnforcerTest {

    private MessageContentTypeEnforcer enforcer = new MessageContentTypeEnforcer();

    private AvroUser avroMessage = new AvroUser("Bob", 30, "black");
    private CompiledSchema<Schema> schema = new CompiledSchema<>(avroMessage.getSchema(), SchemaVersion.valueOf(0));

    @Test
    public void shouldConvertToAvroWhenReceivedJSONOnAvroTopic() throws IOException {
        // when
        byte[] enforcedMessage = enforcer.enforceAvro("application/json", avroMessage.asJson().getBytes(), schema.getSchema());

        // then
        assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
    }

    @Test
    public void shouldStringContentTypeOfAdditionalOptionsWhenInterpretingIt() throws IOException {
        // when
        byte[] enforcedMessage = enforcer.enforceAvro("application/json;encoding=utf-8", avroMessage.asJson().getBytes(), schema.getSchema());

        // then
        assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
    }

    @Test
    public void shouldNotConvertWhenReceivingAvroOnAvroTopic() throws IOException {
        // when
        byte[] enforcedMessage = enforcer.enforceAvro("avro/binary", avroMessage.asBytes(), schema.getSchema());

        // then
        assertThat(enforcedMessage).isEqualTo(avroMessage.asBytes());
    }

}