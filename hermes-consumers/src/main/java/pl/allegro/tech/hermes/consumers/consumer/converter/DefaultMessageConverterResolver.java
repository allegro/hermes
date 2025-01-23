package pl.allegro.tech.hermes.consumers.consumer.converter;

import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public class DefaultMessageConverterResolver implements MessageConverterResolver {

  private final AvroToJsonMessageConverter avroToJsonMessageConverter;
  private final NoOperationMessageConverter noOperationMessageConverter;

  public DefaultMessageConverterResolver(
      AvroToJsonMessageConverter avroToJsonMessageConverter,
      NoOperationMessageConverter noOperationMessageConverter) {
    this.avroToJsonMessageConverter = avroToJsonMessageConverter;
    this.noOperationMessageConverter = noOperationMessageConverter;
  }

  @Override
  public MessageConverter converterFor(Message message, Subscription subscription) {
    if (message.getContentType() == ContentType.AVRO
        && subscription.getContentType() == ContentType.JSON) {
      return avroToJsonMessageConverter;
    }

    return noOperationMessageConverter;
  }
}
