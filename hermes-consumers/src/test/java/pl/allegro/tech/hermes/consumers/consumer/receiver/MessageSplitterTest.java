package pl.allegro.tech.hermes.consumers.consumer.receiver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.consumers.consumer.message.RawMessage;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageSplitterTest {

    private Clock clock = mock(Clock.class);

    private MessageSplitter messageSplitter;

    @Before
    public void setUp() {
        when(clock.getTime()).thenReturn(123L);
        messageSplitter = new MessageSplitter(new ObjectMapper(), clock);
    }

    @Test
    public void shouldReturnEmptyForEmptyMessage() {
        // when
        List<Message> result = messageSplitter.extractMessages(new RawMessage());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldWorkForSingleMessage() {
        // given
        String content = "{\"a\":\"b\"}";

        // when
        List<Message> result = messageSplitter.extractMessages(rawMessage(content));

        // then
        Message message = result.get(0);
        assertThat(message.getData()).isEqualTo(content.getBytes());
        assertThat(message.getOffset()).isEqualTo(15);
        assertThat(message.getReadingTimestamp().get()).isEqualTo(123L);
    }

    @Test
    public void shouldSplitObjectArrayIntoSeparateMessages() {
        //given
        String content1 = "{\"a\":\"b\"}";
        String content2 = "{\"c\":\"d\"}";
        String content = String.format("[%s,%s]", content1, content2);

        //when
        List<Message> result = messageSplitter.extractMessages(rawMessage(content));

        //then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getData()).isEqualTo(content1.getBytes());
        assertThat(result.get(1).getData()).isEqualTo(content2.getBytes());
    }

    @Test
    public void shouldReturnEmptyForMalformedJSON() {
        // when
        List<Message> result = messageSplitter.extractMessages(rawMessage("[ asdfsdf"));

        // then
        assertThat(result).isEmpty();
    }

    private RawMessage rawMessage(String content) {
        return new RawMessage(Optional.of("id"), 15, 0, "topic", content.getBytes(), Optional.of(2132342L));
    }

}
