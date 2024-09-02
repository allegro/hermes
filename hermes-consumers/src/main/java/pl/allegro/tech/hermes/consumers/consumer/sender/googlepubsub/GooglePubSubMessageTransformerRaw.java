package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

class GooglePubSubMessageTransformerRaw implements GooglePubSubMessageTransformer {

  private final MetadataAppender<PubsubMessage> metadataAppender;

  GooglePubSubMessageTransformerRaw(MetadataAppender<PubsubMessage> metadataAppender) {
    this.metadataAppender = metadataAppender;
  }

  @Override
  public PubsubMessage fromHermesMessage(Message message) {
    final PubsubMessage pubsubMessage =
        PubsubMessage.newBuilder().setData(ByteString.copyFrom(message.getData())).build();

    return metadataAppender.append(pubsubMessage, message);
  }
}
