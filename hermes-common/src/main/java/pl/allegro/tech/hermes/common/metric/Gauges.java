package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Gauges {

    public static final String PRODUCER_BUFFER_TOTAL_BYTES = "buffer-total-bytes",
            PRODUCER_BUFFER_AVAILABLE_BYTES = "buffer-available-bytes",
            BATCH_BUFFER_TOTAL_BYTES = "batch-buffer-total-bytes",
            BATCH_BUFFER_AVAILABLE_BYTES = "batch-buffer-available-bytes",
            KAFKA_PRODUCER = "kafka-producer",
            ACK_ALL = "ack-all",
            ACK_LEADER = "ack-leader",

            THREADS = "threads",
            INFLIGHT_REQUESTS = "inflight-requests",
            OUTPUT_RATE = "output-rate." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,
            BACKUP_STORAGE_SIZE = "backup-storage.size",
            MAX_RATE_CALCULATION_DURATION = "consumers-rate.max-rate.coordinator.duration",
            MAX_RATE_VALUE =
                    "consumers-rate.max-rate.node." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".max-rate",
            MAX_RATE_ACTUAL_RATE_VALUE =
                    "consumers-rate.max-rate.node." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".rate",
            RUNNING_CONSUMER_PROCESSES_COUNT = "consumer-processes.running-consumer-processes.count",
            DYING_CONSUMER_PROCESSES_COUNT = "consumer-processes.dying-consumer-processes.count",
            CONSUMER_SENDER_REQUEST_QUEUE_SIZE = "http-clients.request-queue-size",
            CONSUMER_SENDER_HTTP_1_REQUEST_QUEUE_SIZE = "http-clients.http1.request-queue-size",
            CONSUMER_SENDER_HTTP_1_ACTIVE_CONNECTIONS = "http-clients.http1.active-connections",
            CONSUMER_SENDER_HTTP_1_IDLE_CONNECTIONS = "http-clients.http1.idle-connections",
            CONSUMER_SENDER_HTTP_2_REQUEST_QUEUE_SIZE = "http-clients.http2.request-queue-size",
            CONSUMER_SENDER_HTTP_2_CONNECTIONS = "http-clients.http2.connections",
            CONSUMER_SENDER_HTTP_2_PENDING_CONNECTIONS = "http-clients.http2.pending-connections";
}
