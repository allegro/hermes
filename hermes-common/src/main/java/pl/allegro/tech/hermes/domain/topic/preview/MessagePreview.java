package pl.allegro.tech.hermes.domain.topic.preview;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MessagePreview {

  private final byte[] content;

  private final boolean truncated;

  @JsonCreator
  public MessagePreview(
      @JsonProperty("content") byte[] content, @JsonProperty("truncated") boolean truncated) {
    this.content = content;
    this.truncated = truncated;
  }

  public MessagePreview(byte[] content) {
    this(content, false);
  }

  public byte[] getContent() {
    return content;
  }

  public boolean isTruncated() {
    return truncated;
  }
}
