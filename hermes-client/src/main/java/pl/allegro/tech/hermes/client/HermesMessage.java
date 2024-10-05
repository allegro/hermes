package pl.allegro.tech.hermes.client;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** All information Hermes needs to send a message. */
public class HermesMessage {

  public static final String SCHEMA_VERSION_HEADER = "Schema-Version";

  static final String APPLICATION_JSON = "application/json;charset=UTF-8";
  static final String CONTENT_TYPE_HEADER = "Content-Type";
  static final String AVRO_BINARY = "avro/binary";

  private final String topic;
  private final byte[] body;
  private final Map<String, String> headers;

  private HermesMessage(String topic, Map<String, String> headers, byte[] body) {
    this.topic = topic;
    this.headers = headers;
    this.body = body;
  }

  /** Use builder via: HermesMessage#hermesMessage instead. */
  @Deprecated
  public HermesMessage(String topic, String contentType, int schemaVersion, byte[] body) {
    Map<String, String> headers = new HashMap<>();
    headers.put(CONTENT_TYPE_HEADER, contentType);
    headers.put(SCHEMA_VERSION_HEADER, Integer.toString(schemaVersion));

    this.topic = topic;
    this.headers = headers;
    this.body = body;
  }

  /** Use builder via: HermesMessage#hermesMessage instead. */
  @Deprecated
  public HermesMessage(String topic, String contentType, byte[] body) {
    this(topic, contentType, -1, body);
  }

  /**
   * Message on given topic with given MIME Content Type.
   *
   * <p>Use builder via: HermesMessage#hermesMessage instead.
   *
   * @param topic topic name
   * @param contentType MIME content type
   * @param body body which will be translated to byte[] using UTF-8 charset
   */
  @Deprecated
  public HermesMessage(String topic, String contentType, String body) {
    this(topic, contentType, body.getBytes(StandardCharsets.UTF_8));
  }

  /** Use builder via: HermesMessage#hermesMessage instead. */
  @Deprecated
  public HermesMessage(String topic, String body) {
    this(topic, null, body);
  }

  public static Builder hermesMessage(String topic, byte[] content) {
    return new Builder(topic, content);
  }

  public static Builder hermesMessage(String topic, String content) {
    return new Builder(topic, content);
  }

  /**
   * This method modifies the state of HermesMessage in order to avoid additional allocation when
   * appending default values. Using same HermesMessage in multiple HermesClient objects with
   * different defaults is very unlikely, as is sending the same message in multiple threads (which
   * could cause issues with concurrent modification of map).
   */
  static HermesMessage appendDefaults(HermesMessage message, Map<String, String> headers) {
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      if (!message.headers.containsKey(entry.getKey())) {
        message.headers.put(entry.getKey(), entry.getValue());
      }
    }
    return message;
  }

  public String getTopic() {
    return topic;
  }

  public byte[] getBody() {
    return body;
  }

  public String getContentType() {
    return headers.get(CONTENT_TYPE_HEADER);
  }

  public int getSchemaVersion() {
    String schemaVersion = headers.get(SCHEMA_VERSION_HEADER);
    return schemaVersion != null ? Integer.parseInt(schemaVersion) : -1;
  }

  public boolean schemaVersionDefined() {
    return headers.containsKey(SCHEMA_VERSION_HEADER);
  }

  public Map<String, String> getHeaders() {
    return Collections.unmodifiableMap(headers);
  }

  public void consumeHeaders(BiConsumer<String, String> consumer) {
    headers.forEach(consumer);
  }

  @Override
  public String toString() {
    return new String(getBody(), StandardCharsets.UTF_8);
  }

  public static class Builder {

    private final String topic;
    private final byte[] body;
    private final Map<String, String> headers = new HashMap<>();

    private Builder(String topic, byte[] body) {
      this.topic = topic;
      this.body = body;
    }

    private Builder(String topic, String body) {
      this(topic, body.getBytes(StandardCharsets.UTF_8));
    }

    public HermesMessage build() {
      return new HermesMessage(topic, headers, body);
    }

    public Builder json() {
      this.withContentType(APPLICATION_JSON);
      return this;
    }

    public Builder avro(int schemaVersion) {
      this.withContentType(AVRO_BINARY);
      this.withSchemaVersion(schemaVersion);
      return this;
    }

    public Builder withContentType(String contentType) {
      this.headers.put(CONTENT_TYPE_HEADER, contentType);
      return this;
    }

    public Builder withSchemaVersion(int schemaVersion) {
      this.headers.put(SCHEMA_VERSION_HEADER, Integer.toString(schemaVersion));
      return this;
    }

    public Builder withHeader(String header, String value) {
      this.headers.put(header, value);
      return this;
    }
  }
}
