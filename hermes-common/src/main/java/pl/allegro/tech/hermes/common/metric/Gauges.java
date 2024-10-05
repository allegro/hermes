package pl.allegro.tech.hermes.common.metric;

public class Gauges {

  public static final String INFLIGHT_REQUESTS = "inflight-requests";
  public static final String BACKUP_STORAGE_SIZE = "backup-storage.size";

  public static final String CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_ACTIVE_CONNECTIONS =
      "http-clients.serial.http1.active-connections";
  public static final String CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_IDLE_CONNECTIONS =
      "http-clients.serial.http1.idle-connections";

  public static final String CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_ACTIVE_CONNECTIONS =
      "http-clients.batch.http1.active-connections";
  public static final String CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_IDLE_CONNECTIONS =
      "http-clients.batch.http1.idle-connections";

  public static final String CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_CONNECTIONS =
      "http-clients.serial.http2.connections";
  public static final String CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_PENDING_CONNECTIONS =
      "http-clients.serial.http2.pending-connections";
}
