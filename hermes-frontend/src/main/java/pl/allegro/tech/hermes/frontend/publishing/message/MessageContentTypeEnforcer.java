package pl.allegro.tech.hermes.frontend.publishing.message;

import org.apache.avro.Schema;
import org.apache.commons.lang.StringUtils;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.AvroMediaType.AVRO_BINARY;
import static pl.allegro.tech.hermes.api.AvroMediaType.AVRO_JSON;

public class MessageContentTypeEnforcer implements AvroEnforcer {

    private final JsonAvroConverter defaultJsonAvroconverter = new JsonAvroConverter();
    private final AvroEncodedJsonAvroConverter avroEncodedJsonAvroConverter = new AvroEncodedJsonAvroConverter();

    private static final String APPLICATION_JSON_WITH_DELIM = APPLICATION_JSON + ";";
    private static final String AVRO_JSON_WITH_DELIM = AVRO_JSON + ";";
    private static final String AVRO_BINARY_WITH_DELIM = AVRO_BINARY + ";";

    @Override
    public byte[] enforceAvro(String payloadContentType, byte[] data, Schema schema, Topic topic) {
        String contentTypeLowerCase = StringUtils.lowerCase(payloadContentType);
        if (isJSON(contentTypeLowerCase)) {
            return defaultJsonAvroconverter.convertToAvro(data, schema);
        } else if (isAvroJSON(contentTypeLowerCase)) {
            return avroEncodedJsonAvroConverter.convertToAvro(data, schema);
        } else if (isAvroBinary(contentTypeLowerCase)) {
            return data;
        } else {
            throw new UnsupportedContentTypeException(payloadContentType, topic);
        }
    }

    private boolean isJSON(String contentType) {
        return isOfType(contentType, APPLICATION_JSON, APPLICATION_JSON_WITH_DELIM);
    }

    private boolean isAvroJSON(String contentType) {
        return isOfType(contentType, AVRO_JSON, AVRO_JSON_WITH_DELIM);
    }

    private boolean isAvroBinary(String contentType) {
        return isOfType(contentType, AVRO_BINARY, AVRO_BINARY_WITH_DELIM);
    }

    private boolean isOfType(String contentType, String expectedContentType, String expectedWithDelim) {
        return contentType != null && (contentType.equals(expectedContentType) || contentType.startsWith(expectedWithDelim));
    }
}
