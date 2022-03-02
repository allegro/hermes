package pl.allegro.tech.hermes.consumers.consumer.sender;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubMessageSender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.withTestMessage;

@RunWith(MockitoJUnitRunner.class)
public class GooglePubSubMessageSenderTest {

    private static final Message SOME_MESSAGE = withTestMessage().build();

    @InjectMocks
    private GooglePubSubMessageSender messageSender;

    @Before
    public void setUp() {
    }

    @Test
    public void shouldReturnTrueWhenMessageSuccessfullyPublished() throws Exception {
        // when
        CompletableFuture<MessageSendingResult> future = messageSender.send(SOME_MESSAGE);

        // then
        assertTrue(future.get(1, TimeUnit.SECONDS).succeeded());
    }
}
