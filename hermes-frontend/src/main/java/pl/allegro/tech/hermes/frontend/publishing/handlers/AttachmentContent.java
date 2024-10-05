package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.util.AttachmentKey;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

public class AttachmentContent {

  public static final AttachmentKey<AttachmentContent> KEY =
      AttachmentKey.create(AttachmentContent.class);

  private final CachedTopic cachedTopic;
  private final MessageState messageState;
  private final String messageId;
  private byte[] messageContent;
  private volatile TimeoutHolder timeoutHolder;
  private volatile Message message;
  private volatile boolean responseReady;

  public Message getMessage() {
    return message;
  }

  AttachmentContent(CachedTopic cachedTopic, MessageState messageState, String messageId) {
    this.cachedTopic = cachedTopic;
    this.messageState = messageState;
    this.messageId = messageId;
  }

  public MessageState getMessageState() {
    return messageState;
  }

  public TimeoutHolder getTimeoutHolder() {
    return timeoutHolder;
  }

  public void removeTimeout() {
    timeoutHolder.remove();
  }

  public void setTimeoutHolder(TimeoutHolder timeoutHolder) {
    this.timeoutHolder = timeoutHolder;
  }

  public Topic getTopic() {
    return cachedTopic.getTopic();
  }

  public CachedTopic getCachedTopic() {
    return cachedTopic;
  }

  public void setMessageContent(byte[] messageContent) {
    this.messageContent = messageContent;
  }

  public String getMessageId() {
    return messageId;
  }

  public byte[] getMessageContent() {
    return messageContent;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public boolean isResponseReady() {
    return responseReady;
  }

  public void markResponseAsReady() {
    responseReady = true;
  }
}
