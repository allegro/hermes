package pl.allegro.tech.hermes.frontend.metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TopicWithMetrics {

    private final Topic topic;
    private final HermesMetrics hermesMetrics;

    private final Timer topicRequestReadLatencyTimer;
    private final Timer globalRequestReadLatencyTimer;

    private final Timer topicProducerLatencyTimer;
    private final Timer globalProducerLatencyTimer;

    private final Timer topicBrokerLatencyTimer;
    private final Timer globalBrokerLatencyTimer;

    private final Timer topicMessageCreationTimer;
    private final Timer globalMessageCreationTimer;

    private final Counter published;

    private final Map<Integer, MetersPair> httpStatusCodesMeters = new ConcurrentHashMap<>();

    public TopicWithMetrics(Topic topic, HermesMetrics hermesMetrics) {
        this.topic = topic;
        this.hermesMetrics = hermesMetrics;

        globalRequestReadLatencyTimer = hermesMetrics.timer(Timers.PARSING_REQUEST);
        topicRequestReadLatencyTimer = hermesMetrics.timer(Timers.TOPIC_PARSING_REQUEST, topic.getName());

        globalMessageCreationTimer = hermesMetrics.timer(Timers.MESSAGE_CREATION_LATENCY);
        topicMessageCreationTimer = hermesMetrics.timer(Timers.MESSAGE_CREATION_TOPIC_LATENCY, topic.getName());

        published = hermesMetrics.counter(Counters.PUBLISHED, topic.getName());

        if (Topic.Ack.ALL.equals(topic.getAck())) {
            topicProducerLatencyTimer = hermesMetrics.timer(Timers.ACK_ALL_LATENCY);
            globalProducerLatencyTimer = hermesMetrics.timer(Timers.ACK_ALL_TOPIC_LATENCY, topic.getName());

            topicBrokerLatencyTimer = hermesMetrics.timer(Timers.ACK_ALL_BROKER_LATENCY);
            globalBrokerLatencyTimer = hermesMetrics.timer(Timers.ACK_ALL_BROKER_TOPIC_LATENCY, topic.getName());
        } else {
            topicProducerLatencyTimer = hermesMetrics.timer(Timers.ACK_LEADER_LATENCY);
            globalProducerLatencyTimer = hermesMetrics.timer(Timers.ACK_LEADER_TOPIC_LATENCY, topic.getName());

            topicBrokerLatencyTimer = hermesMetrics.timer(Timers.ACK_LEADER_BROKER_LATENCY);
            globalBrokerLatencyTimer = hermesMetrics.timer(Timers.ACK_LEADER_BROKER_TOPIC_LATENCY, topic.getName());
        }
    }

    public Topic getTopic() {
        return topic;
    }

    public StartedTimersPair startRequestReadTimers() {
        return new StartedTimersPair(topicRequestReadLatencyTimer, globalRequestReadLatencyTimer);
    }

    public StartedTimersPair startProducerLatencyTimers() {
        return new StartedTimersPair(topicProducerLatencyTimer, globalProducerLatencyTimer);
    }

    public void markStatusCodeMeter(int status) {
        httpStatusCodesMeters.computeIfAbsent(
                status,
                code -> new MetersPair(
                    hermesMetrics.httpStatusCodeMeter(status),
                    hermesMetrics.httpStatusCodeMeter(status, topic.getName()))
        ).mark();
    }

    public StartedTimersPair startMessageCreationTimers() {
        return new StartedTimersPair(topicMessageCreationTimer, globalMessageCreationTimer);
    }

    public StartedTimersPair startBrokerLatencyTimers() {
        return new StartedTimersPair(topicBrokerLatencyTimer, globalBrokerLatencyTimer);
    }

    public void incrementPublished() {
        published.inc();
    }

}
