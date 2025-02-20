package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

class GooglePubSubMessageTransformerCompression implements GooglePubSubMessageTransformer {

  private final MessageCompressor compressor;
  private final Long compressionThresholdBytes;
  private final GooglePubSubMessageTransformerRaw rawTransformer;
  private final MetadataAppender<PubsubMessage> metadataAppender;

  GooglePubSubMessageTransformerCompression(
      Long compressionThresholdBytes,
      GooglePubSubMessageTransformerRaw rawTransformer,
      MetadataAppender<PubsubMessage> metadataAppender,
      MessageCompressor compressor) {
    this.compressor = compressor;
    this.compressionThresholdBytes = compressionThresholdBytes;
    this.rawTransformer = rawTransformer;
    this.metadataAppender = metadataAppender;
  }

  @Override
  public PubsubMessage fromHermesMessage(Message message) {
    byte[] data = message.getData();
    if (data.length > compressionThresholdBytes) {
      return compressHermesMessage(message);
    } else {
      return rawTransformer.fromHermesMessage(message);
    }
  }

  private PubsubMessage compressHermesMessage(Message message) {
    try {
      final PubsubMessage pubsubMessage =
          PubsubMessage.newBuilder()
              .setData(ByteString.copyFrom(compressor.compress(message.getData())))
              .build();

      return metadataAppender.append(pubsubMessage, message);
    } catch (IOException e) {
      throw new GooglePubSubMessageCompressionException("Error on PubSub message compression", e);
    }
  }
}
