package pl.allegro.tech.hermes.frontend.validator;

import com.google.common.collect.ImmutableList;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.lang.exception.ExceptionUtils;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public class AvroTopicMessageValidator implements TopicMessageValidator {

    @Override
    public void check(Message message, Topic topic) {
        if (ContentType.AVRO != topic.getContentType() || (ContentType.JSON == topic.getContentType() && !topic.isJsonToAvroDryRunEnabled())) {
            return;
        }

        BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(message.getData(), null);
        try {
            new GenericDatumReader<>(message.getSchema()).read(null, binaryDecoder);
        } catch (Exception e) {
            String reason = e.getMessage() == null ? ExceptionUtils.getRootCauseMessage(e) : e.getMessage();
            throw new InvalidMessageException("Could not deserialize avro message with provided schema", ImmutableList.of(reason));
        }
    }
}
