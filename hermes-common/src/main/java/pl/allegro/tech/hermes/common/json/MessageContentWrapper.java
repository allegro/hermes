package pl.allegro.tech.hermes.common.json;

import org.boon.json.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.primitives.Bytes.indexOf;
import static java.lang.String.format;
import static java.util.Arrays.copyOfRange;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGE_CONTENT_ROOT;
import static pl.allegro.tech.hermes.common.config.Configs.METADATA_CONTENT_ROOT;

public class MessageContentWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageContentWrapper.class);

    private static final byte[] SEPARATOR = ",".getBytes(UTF_8);
    private static final byte[] WRAPPED_MARKER = "\"_w\":true".getBytes(UTF_8);
    private static final byte JSON_OPEN = (byte) '{';
    private static final byte JSON_CLOSE = (byte) '}';
    private static final int BRACKET_LENGTH = 1;
    private final ObjectMapper mapper;
    private final byte[] contentRootField;
    private final byte[] metadataRootField;

    @Inject
    public MessageContentWrapper(ConfigFactory config, ObjectMapper mapper) {
        this(config.getStringProperty(MESSAGE_CONTENT_ROOT), config.getStringProperty(METADATA_CONTENT_ROOT), mapper);
    }

    public MessageContentWrapper(String contentRootName, String metadataRootName, ObjectMapper mapper) {
        this.contentRootField = formatNodeKey(contentRootName);
        this.metadataRootField = formatNodeKey(metadataRootName);
        this.mapper = mapper;
    }

    @SuppressWarnings("unchecked")
    public byte[] wrapContent(byte[] json, Map<String, Object> additionalAttributes) throws IOException {
        checkNotNull(json);
        checkNotNull(additionalAttributes);
        return wrapContent(mapper.writeValueAsBytes(additionalAttributes), json);
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
        return isWrapped(json) ? unwrapMessageContent(json) : new UnwrappedMessageContent(json);
    }

    private UnwrappedMessageContent unwrapMessageContent(byte[] json) {
        try {
            int rootIndex = indexOf(json, contentRootField);
            int metadataIndex = indexOf(json, metadataRootField);
            return new UnwrappedMessageContent(unwrapContent(json, rootIndex), unwrapMetadataAsMap(json, metadataIndex, rootIndex));
        } catch (Exception ex) {
            LOGGER.warn("Failed to unwrap message. Returning original json.", ex);
            return new UnwrappedMessageContent(json);
        }
    }

    private byte[] unwrapContent(byte[] json, int rootIndex) {
        return copyOfRange(json, rootIndex + contentRootField.length, json.length - BRACKET_LENGTH);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapMetadataAsMap(byte[] json, int metadataIndexStart, int metadataIndexEnd) {
        try {
            return mapper.readValue(unwrapMetadata(json, metadataIndexStart, metadataIndexEnd), Map.class);
        } catch (Exception ex) {
            LOGGER.warn("Failed to unwrap metadata.", ex);
            return Collections.emptyMap();
        }
    }

    private byte[] unwrapMetadata(byte[] json, int metadataIndexStart, int metadataIndexEnd) {
        return copyOfRange(json, metadataIndexStart + metadataRootField.length, metadataIndexEnd + BRACKET_LENGTH);
    }

    private boolean isWrapped(byte[] json) {
        return indexOf(json, WRAPPED_MARKER) > 0;
    }

    private byte[] formatNodeKey(String keyName) {
        return format("\"%s\":", keyName).getBytes(UTF_8);
    }

}
