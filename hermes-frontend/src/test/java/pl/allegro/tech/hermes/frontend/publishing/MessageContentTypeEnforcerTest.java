package pl.allegro.tech.hermes.frontend.publishing;

import org.apache.avro.Schema;
import org.junit.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageContentTypeEnforcerTest {

    @SuppressWarnings("unchecked")
    private SchemaRepository<Schema> schemaRepository = mock(SchemaRepository.class);

    private MessageContentTypeEnforcer enforcer = new MessageContentTypeEnforcer(schemaRepository);

    private AvroUser avroMessage = new AvroUser("Bob", 30, "black");

    @Test
    public void shouldConvertToAvroWhenReceivedJSONOnAvroTopic() throws IOException {
        // given
        Topic topic = Topic.Builder.topic().withName("enforcer", "json2avro").withContentType(ContentType.AVRO).build();
        Message message = new Message("1", avroMessage.asJson().getBytes(), 1234);

        when(schemaRepository.getSchema(topic)).thenReturn(avroMessage.getSchema());

        // when
        Message enforcedMessage = enforcer.enforce("application/json", message, topic);

        // then
        assertThat(enforcedMessage.getData()).isEqualTo(avroMessage.asBytes());
    }

    @Test
    public void shouldStringContentTypeOfAdditionalOptionsWhenInterpretingIt() throws IOException {
        // given
        Topic topic = Topic.Builder.topic().withName("enforcer", "json2avro-encoding").withContentType(ContentType.AVRO).build();
        Message message = new Message("1", avroMessage.asJson().getBytes(), 1234);

        when(schemaRepository.getSchema(topic)).thenReturn(avroMessage.getSchema());

        // when
        Message enforcedMessage = enforcer.enforce("application/json;encoding=utf-8", message, topic);

        // then
        assertThat(enforcedMessage.getData()).isEqualTo(avroMessage.asBytes());
    }

    @Test
    public void shouldNotConvertWhenReceivingJSONOnJSONTopic() throws IOException {
        // given
        Topic topic = Topic.Builder.topic().withName("enforcer", "json2json").withContentType(ContentType.JSON).build();
        Message message = new Message("1", avroMessage.asJson().getBytes(), 1234);

        // when
        Message enforcedMessage = enforcer.enforce("application/json", message, topic);

        // then
        assertThat(enforcedMessage.getData()).isEqualTo(avroMessage.asJson().getBytes());
    }

    @Test
    public void shouldNotConvertWhenReceivingAvroOnAvroTopic() throws IOException {
        // given
        Topic topic = Topic.Builder.topic().withName("enforcer", "avro2avro").withContentType(ContentType.AVRO).build();
        Message message = new Message("1", avroMessage.asBytes(), 1234);

        // when
        Message enforcedMessage = enforcer.enforce("avro/binary", message, topic);

        // then
        assertThat(enforcedMessage.getData()).isEqualTo(avroMessage.asBytes());
    }

}