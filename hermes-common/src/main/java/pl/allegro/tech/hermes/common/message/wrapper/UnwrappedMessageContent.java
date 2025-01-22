package pl.allegro.tech.hermes.common.message.wrapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.schema.CompiledSchema;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public class UnwrappedMessageContent {

  private final MessageMetadata messageMetadata;
  private final byte[] content;
  private final Optional<CompiledSchema<Schema>> schema;

  public UnwrappedMessageContent(MessageMetadata messageMetadata, byte[] content) {
    this.messageMetadata = messageMetadata;
    this.content = content;
    this.schema = Optional.empty();
  }

  public UnwrappedMessageContent(
      MessageMetadata messageMetadata, byte[] content, CompiledSchema<Schema> schema) {
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

  public Optional<CompiledSchema<Schema>> getSchema() {
    return schema;
  }
}
