package pl.allegro.tech.hermes.common.message.wrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Ignore;
import org.junit.Test;

public class JsonMessageContentWrapperTest {

  private final byte[] testContent = "{\"key\":\"value\"}".getBytes();
  private final ObjectMapper mapper = new ObjectMapper();
  private final MessageMetadata metadata =
      new MessageMetadata(
          System.currentTimeMillis(), "14cf17ea-f1ea-a464-6bd6478615bb", ImmutableMap.of());
  private final Map<String, Object> metadataAsMap =
      ImmutableMap.of(
          "timestamp",
          metadata.getTimestamp(),
          "id",
          metadata.getId(),
          "externalMetadata",
          Collections.emptyMap());
  private final MapEntry unwrappingMarker = entry("_w", true);
  private final MapEntry content = entry("message", readMap(testContent));

  private final JsonMessageContentWrapper contentWrapper =
      new JsonMessageContentWrapper(content.key.toString(), "metadata", mapper);
  private Map<String, String> externalMetadata = ImmutableMap.of();

  @Test
  @SuppressWarnings("unchecked")
  public void shouldWrapJsonWithMetadata() {
    // when
    byte[] result =
        contentWrapper.wrapContent(
            testContent, metadata.getId(), metadata.getTimestamp(), externalMetadata);

    // then
    assertThat(readMap(result))
        .containsExactly(unwrappingMarker, entry("metadata", metadataAsMap), content);
  }

  @Test
  public void shouldUnwrapMessageWithMetadata() {
    // when
    UnwrappedMessageContent result =
        contentWrapper.unwrapContent(
            contentWrapper.wrapContent(
                testContent, metadata.getId(), metadata.getTimestamp(), externalMetadata));

    // then
    assertThat(result.getContent()).isEqualTo(testContent);
    assertThat(result.getMessageMetadata()).isEqualTo(metadata);
  }

  @Test
  public void shouldTolerateUnwrappingUnwrappedMessage() {
    // when
    UnwrappedMessageContent result = contentWrapper.unwrapContent(testContent);

    // then
    assertThat(result.getMessageMetadata().getId()).isNotEmpty();
    assertThat(result.getMessageMetadata().getTimestamp()).isEqualTo(1L);
  }

  @Ignore
  @Test(expected = UnwrappingException.class)
  public void shouldThrowExceptionWhenMetadataNotFound() {
    contentWrapper.unwrapContent(testContent);
  }

  private Map<String, Object> readMap(byte[] result) {
    try {
      return mapper.readValue(new String(result), new TypeReference<Map<String, Object>>() {});
    } catch (IOException e) {
      throw new IllegalStateException("Error while reading map", e);
    }
  }
}
