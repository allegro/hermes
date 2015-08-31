package pl.allegro.tech.hermes.common.message.wrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.primitives.Bytes.indexOf;
import static java.lang.String.format;
import static java.util.Arrays.copyOfRange;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGE_CONTENT_ROOT;
import static pl.allegro.tech.hermes.common.config.Configs.METADATA_CONTENT_ROOT;

public class JsonMessageContentWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageContentWrapper.class);

    private static final byte[] SEPARATOR = ",".getBytes(UTF_8);
    private static final byte[] WRAPPED_MARKER = "\"_w\":true".getBytes(UTF_8);
    private static final byte JSON_OPEN = (byte) '{';
    private static final byte JSON_CLOSE = (byte) '}';
    private static final int BRACKET_LENGTH = 1;
    private final ObjectMapper mapper;
    private final byte[] contentRootField;
    private final byte[] metadataRootField;

    @Inject
    public JsonMessageContentWrapper(ConfigFactory config, ObjectMapper mapper) {
        this(config.getStringProperty(MESSAGE_CONTENT_ROOT), config.getStringProperty(METADATA_CONTENT_ROOT), mapper);
    }

    public JsonMessageContentWrapper(String contentRootName, String metadataRootName, ObjectMapper mapper) {
        this.contentRootField = formatNodeKey(contentRootName);
        this.metadataRootField = formatNodeKey(metadataRootName);
        this.mapper = mapper;
    }

    byte[] wrapContent(byte[] json, String id, long timestamp) {
        try {
            return wrapContent(mapper.writeValueAsBytes(new MessageMetadata(timestamp, id)), json);
        } catch (IOException e) {
            throw new WrappingException("Could not wrap json message", e);
        }
    }

    private byte[] wrapContent(byte[] attributes, byte[] message) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(JSON_OPEN);
        stream.write(WRAPPED_MARKER);
        stream.write(SEPARATOR);
        stream.write(metadataRootField);
        stream.write(attributes);
        stream.write(SEPARATOR);
        stream.write(contentRootField);
        stream.write(message);
        stream.write(JSON_CLOSE);
        return stream.toByteArray();
    }

    public UnwrappedMessageContent unwrapContent(byte[] json) {
        if (isWrapped(json)) {
            return unwrapMessageContent(json);
        } else {
            UUID id = UUID.randomUUID();
            LOGGER.warn("Unwrapped message read by consumer (size=%s, id=%s).", json.length, id.toString());
            return new UnwrappedMessageContent(new MessageMetadata(1L, id.toString()), json);
        }
    }

    private UnwrappedMessageContent unwrapMessageContent(byte[] json) {
        int rootIndex = indexOf(json, contentRootField);
        int metadataIndex = indexOf(json, metadataRootField);
        try {
            return new UnwrappedMessageContent(unwrapMesssageMetadata(json, metadataIndex, rootIndex), unwrapContent(json, rootIndex));
        } catch (Exception exception) {
            throw new UnwrappingException("Could not unwrap json message", exception);
        }
    }

    private byte[] unwrapContent(byte[] json, int rootIndex) {
        return copyOfRange(json, rootIndex + contentRootField.length, json.length - BRACKET_LENGTH);
    }

    private MessageMetadata unwrapMesssageMetadata(byte[] json, int metadataIndexStart, int metadataIndexEnd) throws IOException {
        return mapper.readValue(unwrapMetadataBytes(json, metadataIndexStart, metadataIndexEnd), MessageMetadata.class);
    }

    private byte[] unwrapMetadataBytes(byte[] json, int metadataIndexStart, int metadataIndexEnd) {
        return copyOfRange(json, metadataIndexStart + metadataRootField.length, metadataIndexEnd + BRACKET_LENGTH);
    }

    private byte[] formatNodeKey(String keyName) {
        return format("\"%s\":", keyName).getBytes(UTF_8);
    }

    private boolean isWrapped(byte[] json) {
        return indexOf(json, WRAPPED_MARKER) > 0;
    }

}
