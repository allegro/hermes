package pl.allegro.tech.hermes.common.message.wrapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import java.util.Optional;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public class UnwrappedMessageContent {

    private final MessageMetadata messageMetadata;
    private final byte[] content;
    private final Optional<CompiledSchema<?>> schema;

    public UnwrappedMessageContent(MessageMetadata messageMetadata, byte[] content) {
        this.messageMetadata = messageMetadata;
        this.content = content;
        this.schema = Optional.empty();
    }

    public UnwrappedMessageContent(MessageMetadata messageMetadata, byte[] content, CompiledSchema schema) {
        this.messageMetadata = messageMetadata;
        this.content = content;
        this.schema = Optional.of(schema);
    }

    public byte[] getContent() {
        return content;
    }

    public MessageMetadata getMessageMetadata() {
        return messageMetadata;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<CompiledSchema<T>> getSchema() {
        return schema.map(schema -> (CompiledSchema<T>)schema);
    }
}
