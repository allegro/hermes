package pl.allegro.tech.hermes.common.json;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.data.MapEntry;
import org.boon.json.JsonFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static pl.allegro.tech.hermes.common.json.JsonReader.readMap;

public class MessageContentWrapperTest {

    private final static byte[] CONTENT = "{\"key\":\"value\"}".getBytes();
    
    Map<String, Object> metadata = ImmutableMap.of("timestamp", 123456789, "id", "14cf17ea-f1ea-a464-6bd6478615bb");
    MapEntry unwrappingMarker = entry("_w", true);
    MapEntry content = entry("message", readMap(CONTENT));

    MessageContentWrapper contentWrapper = new MessageContentWrapper(content.key.toString(), "metadata", JsonFactory.create());

    @Test
    public void shouldWrapJsonWithAdditionalParametersGivenAsMap() throws IOException {
        //when
        byte[] result = contentWrapper.wrapContent(CONTENT, metadata);

        //then
        assertThat(readMap(result)).containsExactly(unwrappingMarker, entry("metadata", metadata), content);
    }

    @Test
    public void shouldWrapJsonWithSingleParameterGivenAsMap() throws IOException {
        //given
        Map<String, Object> single = ImmutableMap.<String, Object>of("single", "value");

        //when
        byte[] result = contentWrapper.wrapContent(CONTENT, single);

        //then
        assertThat(readMap(result)).containsExactly(unwrappingMarker, entry("metadata", single), content);
    }

    @Test
    public void shouldWrapJsonEvenIfThereAreNoParametersGiven() throws IOException {
        //when
        byte[] result = contentWrapper.wrapContent(CONTENT, new HashMap<String, Object>());

        //then
        assertThat(readMap(result)).contains(unwrappingMarker, content);
    }

    @Test
    public void shouldUnwrapWrappedContent() throws IOException {
        //when
        byte[] result = contentWrapper.unwrapContent(contentWrapper.wrapContent(CONTENT, metadata)).getContent();

        //then
        assertThat(new String(result)).isEqualTo(new String(CONTENT));
    }

    @Test
    public void shouldReturnOriginalJsonIfMessageCouldNotBeUnwrapped() {
        //when
        byte[] result = contentWrapper.unwrapContent(CONTENT).getContent();

        //then
        assertThat(new String(result)).isEqualTo(new String(CONTENT));
    }

    @Test
    public void shouldUnwrapMetadata() throws IOException {
        //when
        UnwrappedMessageContent result = contentWrapper.unwrapContent(contentWrapper.wrapContent(CONTENT, metadata));

        //then
        assertThat(result.areMetadataAvailable()).isTrue();
        assertThat(result.getLongFromMetadata("timestamp").get()).isEqualTo(123456789);
        assertThat(result.getStringFromMetadata("id").get()).isEqualTo("14cf17ea-f1ea-a464-6bd6478615bb");
    }

    @Test
    public void shouldReturnEmptyMetadata() throws IOException {
        //when
        UnwrappedMessageContent result = contentWrapper.unwrapContent(contentWrapper.wrapContent(CONTENT, new HashMap<String, Object>()));

        //then
        assertThat(result.areMetadataAvailable()).isFalse();
    }
}
