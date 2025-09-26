package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleRecipientMessageSenderAdapter;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroDataWriterPool;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroMessageTransformer;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonDataWriterPool;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonMessageTransformer;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json.GoogleBigQueryJsonSender;

public class GoogleBigQueryMessageSenderProvider implements ProtocolMessageSenderProvider {

  public static final String SUPPORTED_PROTOCOL = "googlebigquery";

  private final GoogleBigQuerySenderTargetResolver targetResolver;
  private final GoogleBigQueryJsonMessageTransformer jsonMessageTransformer;
  private final GoogleBigQueryAvroMessageTransformer avroMessageTransformer;
  private final GoogleBigQueryAvroDataWriterPool avroDataWriterPool;
  private final GoogleBigQueryJsonDataWriterPool jsonDataWriterPool;

  public GoogleBigQueryMessageSenderProvider(
      GoogleBigQuerySenderTargetResolver targetResolver,
      GoogleBigQueryJsonMessageTransformer jsonMessageTransformer,
      GoogleBigQueryAvroMessageTransformer avroMessageTransformer,
      GoogleBigQueryJsonDataWriterPool jsonDataWriterPool,
      GoogleBigQueryAvroDataWriterPool avroDataWriterPool) {
    this.targetResolver = targetResolver;
    this.jsonMessageTransformer = jsonMessageTransformer;
    this.jsonDataWriterPool = jsonDataWriterPool;
    this.avroMessageTransformer = avroMessageTransformer;
    this.avroDataWriterPool = avroDataWriterPool;
  }

  @Override
  public MessageSender create(
      Subscription subscription, ResilientMessageSender resilientMessageSender) {
    GoogleBigQuerySenderTarget target = targetResolver.resolve(subscription.getEndpoint());
    CompletableFutureAwareMessageSender sender;
    if (subscription.getContentType().equals(ContentType.JSON)) {
      sender = new GoogleBigQueryJsonSender(jsonMessageTransformer, target, jsonDataWriterPool);
    } else {
      sender =
          new GoogleBigQueryAvroSender(avroMessageTransformer, subscription, avroDataWriterPool);
    }

    return new SingleRecipientMessageSenderAdapter(sender, resilientMessageSender);
  }

  @Override
  public Set<String> getSupportedProtocols() {
    return ImmutableSet.of(SUPPORTED_PROTOCOL);
  }

  @Override
  public void start() throws Exception {}

  @Override
  public void stop() throws Exception {
    jsonDataWriterPool.shutdown();
    avroDataWriterPool.shutdown();
  }
}
