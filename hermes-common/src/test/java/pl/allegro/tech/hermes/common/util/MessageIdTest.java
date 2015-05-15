package pl.allegro.tech.hermes.common.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageIdTest {

    @Test
    public void shouldCreateHash() {
        // given
        String topic = "example.topic";
        long offset = 100L;

        String expectedHash = "53e12118212f595641af06002643e428";

        // when
        String actualHash = MessageId.forTopicAndOffset(topic, offset);

        // then
        assertThat(actualHash).isEqualTo(expectedHash);
    }
}