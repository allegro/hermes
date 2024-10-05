package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TOPIC_NAME;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.succeededResult;

import java.util.concurrent.CompletableFuture;
import javax.jms.BytesMessage;
import javax.jms.CompletionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

public class JmsMessageSender implements CompletableFutureAwareMessageSender {

  private static final Logger logger = LoggerFactory.getLogger(JmsMessageSender.class);

  private final String topicName;
  private final JMSContext jmsContext;
  private final MetadataAppender<javax.jms.Message> metadataAppender;

  public JmsMessageSender(
      JMSContext jmsContext,
      String destinationTopic,
      MetadataAppender<javax.jms.Message> metadataAppender) {
    this.jmsContext = jmsContext;
    this.topicName = destinationTopic;
    this.metadataAppender = metadataAppender;
  }

  @Override
  public void stop() {
    jmsContext.close();
  }

  @Override
  public void send(Message msg, final CompletableFuture<MessageSendingResult> resultFuture) {
    try {
      BytesMessage message = jmsContext.createBytesMessage();
      message.writeBytes(msg.getData());
      message.setStringProperty(TOPIC_NAME.getCamelCaseName(), msg.getTopic());
      message.setStringProperty(MESSAGE_ID.getCamelCaseName(), msg.getId());

      metadataAppender.append(message, msg);

      CompletionListener asyncListener =
          new CompletionListener() {
            @Override
            public void onCompletion(javax.jms.Message message) {
              resultFuture.complete(succeededResult());
            }

            @Override
            public void onException(javax.jms.Message message, Exception exception) {
              logger.warn(
                  String.format("Exception while sending message to topic %s", topicName),
                  exception);
              resultFuture.complete(failedResult(exception));
            }
          };
      jmsContext
          .createProducer()
          .setAsync(asyncListener)
          .send(jmsContext.createTopic(topicName), message);
    } catch (JMSException | JMSRuntimeException e) {
      resultFuture.complete(failedResult(e));
    }
  }
}
