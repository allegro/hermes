package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import org.json.JSONObject;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface GoogleBigQueryMessageTransformer<T> {

    T fromHermesMessage(Message message);
}
