package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.AbstractMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import javax.jms.BytesMessage;
import javax.jms.CompletionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;

import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TOPIC_NAME;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.succeededResult;

public class JmsMessageSender extends AbstractMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(JmsMessageSender.class);

    private final String topicName;

    private final JMSContext jmsContext;

    public JmsMessageSender(JMSContext jmsContext, String destinationTopic) {
        this.jmsContext = jmsContext;
        this.topicName = destinationTopic;
    }

    @Override
    public void stop() {
        jmsContext.close();
    }

    @Override
    protected void sendMessage(Message msg, final SettableFuture<MessageSendingResult> resultFuture) {
        try {
            BytesMessage message = jmsContext.createBytesMessage();
            message.writeBytes(msg.getData());
            message.setStringProperty(TOPIC_NAME.getCamelCaseName(), msg.getTopic());
            message.setStringProperty(MESSAGE_ID.getCamelCaseName(), msg.getId().orElse("unavailable"));

            CompletionListener asyncListener = new CompletionListener() {
                @Override
                public void onCompletion(javax.jms.Message message) {
                    resultFuture.set(succeededResult());
                }

                @Override
                public void onException(javax.jms.Message message, Exception exception) {
                    logger.warn(String.format("Exception while sending message to topic %s", topicName), exception);
                    resultFuture.set(failedResult(exception));
                }
            };
            jmsContext.createProducer()
                    .setAsync(asyncListener)
                    .send(jmsContext.createTopic(topicName), message);
        } catch (JMSException | JMSRuntimeException e) {
            resultFuture.set(failedResult(e));
        }
    }

}
