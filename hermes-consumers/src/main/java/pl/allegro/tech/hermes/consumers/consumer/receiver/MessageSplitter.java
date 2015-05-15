package pl.allegro.tech.hermes.consumers.consumer.receiver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.consumers.consumer.message.MessageStatus;
import pl.allegro.tech.hermes.consumers.consumer.message.RawMessage;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MessageSplitter {

    private static final int LEFT_SQUARE_BRACKET_CHAR = 91;
    private ObjectMapper objectMapper;
    private Clock clock;

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSplitter.class);

    @Inject
    public MessageSplitter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    List<Message> extractMessages(RawMessage message) {
        if (message.getStatus() == MessageStatus.EMPTY) {
            return Collections.emptyList();
        }
        try {
            Optional<Long> readingTimestamp = Optional.of(clock.getTime());
            if (message.getData()[0] == LEFT_SQUARE_BRACKET_CHAR) {
                JsonNode node = objectMapper.readTree(message.getData());
                List<Message> result = new ArrayList<>();
                for (JsonNode childNode : node) {
                    result.add(new Message(
                            message.getId(),
                            message.getOffset(),
                            message.getPartition(),
                            message.getTopic(),
                            childNode.toString().getBytes(Charset.forName("UTF-8")),
                            message.getTimestamp(),
                            readingTimestamp
                            ));
                }
                return result;
            } else {
                return Collections.singletonList(new Message(
                        message.getId(),
                        message.getOffset(),
                        message.getPartition(),
                        message.getTopic(),
                        message.getData(),
                        message.getTimestamp(),
                        readingTimestamp
                        ));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse message: " + new String(message.getData()), e);
            return Collections.emptyList();
        }
    }
}
