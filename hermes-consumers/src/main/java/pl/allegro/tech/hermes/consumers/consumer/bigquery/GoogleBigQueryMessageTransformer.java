package pl.allegro.tech.hermes.consumers.consumer.bigquery;

import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface GoogleBigQueryMessageTransformer<T> {

  T fromHermesMessage(Message message);
}
