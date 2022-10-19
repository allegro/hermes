package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Gauges {

    public static final String KAFKA_PRODUCER = "kafka-producer.";
    public static final String ACK_LEADER = "ack-leader.";
    public static final String ACK_ALL = "ack-all.";

    public static final String ACK_ALL_BUFFER_TOTAL_BYTES = KAFKA_PRODUCER + ACK_ALL + "buffer-total-bytes";
    public static final String ACK_ALL_BUFFER_AVAILABLE_BYTES = KAFKA_PRODUCER + ACK_ALL + "buffer-available-bytes";
    public static final String ACK_ALL_CONFIRMS_METADATA_AGE = KAFKA_PRODUCER + ACK_ALL + "metadata-age";
    public static final String ACK_ALL_RECORD_QUEUE_TIME_MAX = KAFKA_PRODUCER + ACK_ALL + "record-queue-time-max";
    public static final String ACK_ALL_COMPRESSION_RATE = KAFKA_PRODUCER + ACK_ALL + "compression-rate-avg";
    public static final String ACK_ALL_FAILED_BATCHES_TOTAL = KAFKA_PRODUCER + ACK_ALL + "failed-batches-total";

    public static final String ACK_LEADER_FAILED_BATCHES_TOTAL = KAFKA_PRODUCER + ACK_LEADER + "failed-batches-total";
    public static final String ACK_LEADER_BUFFER_TOTAL_BYTES = KAFKA_PRODUCER + ACK_LEADER + "buffer-total-bytes";
    public static final String ACK_LEADER_METADATA_AGE = KAFKA_PRODUCER + ACK_LEADER + "metadata-age";
    public static final String ACK_LEADER_RECORD_QUEUE_TIME_MAX = KAFKA_PRODUCER + ACK_LEADER + "record-queue-time-max";
    public static final String ACK_LEADER_BUFFER_AVAILABLE_BYTES = KAFKA_PRODUCER + ACK_LEADER + "buffer-available-bytes";
    public static final String ACK_LEADER_COMPRESSION_RATE = KAFKA_PRODUCER + ACK_LEADER + "compression-rate-avg";

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
    public static final String CONSUMER_SENDER_HTTP_1_REQUEST_QUEUE_SIZE = "http-clients.http1.request-queue-size";
    public static final String CONSUMER_SENDER_HTTP_1_ACTIVE_CONNECTIONS = "http-clients.http1.active-connections";
    public static final String CONSUMER_SENDER_HTTP_1_IDLE_CONNECTIONS = "http-clients.http1.idle-connections";
    public static final String CONSUMER_SENDER_HTTP_2_REQUEST_QUEUE_SIZE = "http-clients.http2.request-queue-size";
    public static final String CONSUMER_SENDER_HTTP_2_CONNECTIONS = "http-clients.http2.connections";
    public static final String CONSUMER_SENDER_HTTP_2_PENDING_CONNECTIONS = "http-clients.http2.pending-connections";
}
