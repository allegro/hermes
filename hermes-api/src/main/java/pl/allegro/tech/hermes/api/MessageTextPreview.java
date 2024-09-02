package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageTextPreview {

  private final String content;

  private final boolean truncated;

  @JsonCreator
  public MessageTextPreview(
      @JsonProperty("content") String content, @JsonProperty("truncated") boolean truncated) {
    this.content = content;
    this.truncated = truncated;
  }

  public String getContent() {
    return content;
  }

  public boolean isTruncated() {
    return truncated;
  }
}
