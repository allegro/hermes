package pl.allegro.tech.hermes.common.message.wrapper;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AvroMessageContentWrapperTest {

    private AvroMessageContentWrapper avroMessageContentWrapper;

    @Before
    public void setup() throws IOException {
        avroMessageContentWrapper = new AvroMessageContentWrapper();
    }

    @Test
    public void shouldWrapAndUnwrapAvroMessageWithMetadata() throws IOException {
        // given
        String id = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        byte [] content = "message".getBytes();

        // when
        byte [] wrappedMessage = avroMessageContentWrapper.wrapContent(content, id, timestamp);
        MessageWithMetadata messageWithMetadata = avroMessageContentWrapper.unwrapContent(wrappedMessage);

        // then
        assertThat(messageWithMetadata.getMessageMetadata().getId()).isEqualTo(id);
        assertThat(messageWithMetadata.getMessageMetadata().getTimestamp()).isEqualTo(timestamp);
        assertThat(messageWithMetadata.getContent()).isEqualTo(content);
    }

}