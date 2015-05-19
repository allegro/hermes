package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import javax.jms.BytesMessage;
import javax.jms.CompletionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;

@RunWith(MockitoJUnitRunner.class)
public class JmsMessageSenderTest {

    private static final Message SOME_MESSAGE = new Message(
            Optional.of("id"), 0, 0, "topic", "aaaaaaaaaaaaaaaa".getBytes(), Optional.of(1214323L), Optional.of(12143234L)
    );

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JMSContext jmsContextMock;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JMSProducer jmsProducerMock;

    @Mock
    private BytesMessage messageMock;

    @Mock
    private ConfigFactory configFactoryMock;

    @InjectMocks
    private JmsMessageSender messageSender;

    @Before
    public void setUp() throws Exception {
        when(jmsContextMock.createBytesMessage()).thenReturn(messageMock);
        when(jmsContextMock.createProducer()).thenReturn(jmsProducerMock);
    }

    @Test
    public void shouldReturnTrueWhenMessageSuccessfullyPublished() throws Exception {
        // when
        ListenableFuture<MessageSendingResult> future = messageSender.send(SOME_MESSAGE);

        // then
        ArgumentCaptor<CompletionListener> listenerCaptor = ArgumentCaptor.forClass(CompletionListener.class);
        verify(jmsProducerMock).setAsync(listenerCaptor.capture());
        listenerCaptor.getValue().onCompletion(messageMock);
        assertTrue(future.get(1, TimeUnit.SECONDS).succeeded());
    }

    @Test
    public void shouldReturnFalseWhenOnExceptionCalledOnListener() throws Exception {
        // when
        ListenableFuture<MessageSendingResult> future = messageSender.send(SOME_MESSAGE);

        // then
        ArgumentCaptor<CompletionListener> listenerCaptor = ArgumentCaptor.forClass(CompletionListener.class);
        verify(jmsProducerMock).setAsync(listenerCaptor.capture());
        listenerCaptor.getValue().onException(messageMock, new RuntimeException());
        assertFalse(future.get(1, TimeUnit.SECONDS).succeeded());
    }

    @Test
    public void shouldReturnFalseWhenJMSThrowsCheckedException() throws Exception {
        // given
        doThrow(new JMSException("test")).when(messageMock).writeBytes(SOME_MESSAGE.getData());

        // when
        ListenableFuture<MessageSendingResult> future = messageSender.send(SOME_MESSAGE);

        // then
        assertFalse(future.get(1, TimeUnit.SECONDS).succeeded());
    }

    @Test
    public void shouldReturnFalseWhenJMSThrowsRuntimeException() throws Exception {
        // given
        doThrow(new JMSRuntimeException("test")).when(jmsContextMock).createProducer();

        // when
        ListenableFuture<MessageSendingResult> future = messageSender.send(SOME_MESSAGE);

        // then
        assertFalse(future.get(1, TimeUnit.SECONDS).succeeded());
    }

    @Test
    public void shouldSetMessageIdInProperty() throws JMSException {
        // when
        messageSender.send(SOME_MESSAGE);

        // then
        verify(messageMock).setStringProperty(MESSAGE_ID.getCamelCaseName(), "id");
    }
}
