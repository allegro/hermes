package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import org.json.JSONObject;
import org.json.JSONTokener;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.bigquery.GoogleBigQueryMessageTransformer;

public class GoogleBigQueryJsonMessageTransformer implements GoogleBigQueryMessageTransformer {
  @Override
  public JSONObject fromHermesMessage(Message message) {
    Preconditions.checkArgument(message.getContentType().equals(ContentType.JSON));

    String jsonString = new String(message.getData(), Charsets.UTF_8);
    return new JSONObject(new JSONTokener(jsonString));
  }
}
