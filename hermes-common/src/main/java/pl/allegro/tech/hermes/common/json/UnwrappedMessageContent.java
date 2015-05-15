package pl.allegro.tech.hermes.common.json;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public class UnwrappedMessageContent {

    private byte[] content;
    private Map<String, Object> metadata;

    public UnwrappedMessageContent(byte[] content, Map<String, Object> metadata) {
        this.content = content;
        this.metadata = metadata;
    }

    public UnwrappedMessageContent(byte[] json) {
        this(json, Collections.<String, Object>emptyMap());
    }

    public byte[] getContent() {
        return Arrays.copyOf(content, content.length);
    }

    public Optional<Long> getLongFromMetadata(String field) {
        return getStringFromMetadata(field).map(Long::parseLong);
    }

    public Optional<String> getStringFromMetadata(String field) {
        return isFieldAvailable(field) ? Optional.of(metadata.get(field).toString()) : Optional.empty();
    }

    public boolean areMetadataAvailable() {
        return !metadata.isEmpty();
    }

    private boolean isFieldAvailable(String field) {
        return areMetadataAvailable() && metadata.containsKey(field);
    }
}
