package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Gauges {
    public static final String BATCH_BUFFER_TOTAL_BYTES = "batch-buffer-total-bytes";
    public static final String BATCH_BUFFER_AVAILABLE_BYTES = "batch-buffer-available-bytes";

    public static final String THREADS = "threads";
    public static final String INFLIGHT_REQUESTS = "inflight-requests";
    public static final String OUTPUT_RATE = "output-rate." + GROUP + "." + TOPIC + "." + SUBSCRIPTION;
    public static final String BACKUP_STORAGE_SIZE = "backup-storage.size";
    public static final String MAX_RATE_CALCULATION_DURATION = "consumers-rate.max-rate.coordinator.duration";
    public static final String MAX_RATE_VALUE =
                    "consumers-rate.max-rate.node." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".max-rate";
    public static final String MAX_RATE_ACTUAL_RATE_VALUE =
                    "consumers-rate.max-rate.node." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".rate";
    public static final String RUNNING_CONSUMER_PROCESSES_COUNT = "consumer-processes.running-consumer-processes.count";
    public static final String DYING_CONSUMER_PROCESSES_COUNT = "consumer-processes.dying-consumer-processes.count";
    public static final String CONSUMER_SENDER_REQUEST_QUEUE_SIZE = "http-clients.request-queue-size";

    public static final String CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_REQUEST_QUEUE_SIZE = "http-clients.serial.http1.request-queue-size";
    public static final String CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_ACTIVE_CONNECTIONS = "http-clients.serial.http1.active-connections";
    public static final String CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_IDLE_CONNECTIONS = "http-clients.serial.http1.idle-connections";

    public static final String CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_REQUEST_QUEUE_SIZE = "http-clients.batch.http1.request-queue-size";
    public static final String CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_ACTIVE_CONNECTIONS = "http-clients.batch.http1.active-connections";
    public static final String CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_IDLE_CONNECTIONS = "http-clients.batch.http1.idle-connections";

    public static final String CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_REQUEST_QUEUE_SIZE = "http-clients.serial.http2.request-queue-size";
    public static final String CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_CONNECTIONS = "http-clients.serial.http2.connections";
    public static final String CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_PENDING_CONNECTIONS = "http-clients.serial.http2.pending-connections";

    public static final String INFLIGHT = "inflight." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".count";
    public static final String OFFSET_QUEUE = "offset-queue." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".count";
}
