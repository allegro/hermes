package pl.allegro.tech.hermes.frontend.publishing;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.common.metric.timer.ProducerAckAllLatencyTimer;
import pl.allegro.tech.hermes.common.metric.timer.ProducerAckLeaderLatencyTimer;
import pl.allegro.tech.hermes.common.metric.timer.ProducerLatencyTimer;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.ws.rs.core.Response.Status.Family;

class MetricsAsyncListener implements AsyncListener {

    private final HermesMetrics hermesMetrics;
    private final TopicName topicName;
    private final ProducerLatencyTimer producerLatencyTimer;

    MetricsAsyncListener(HermesMetrics hermesMetrics, TopicName topicName, Topic.Ack ack) {
        this.hermesMetrics = hermesMetrics;
        this.topicName = topicName;
        this.producerLatencyTimer = latencyTimer(hermesMetrics, topicName, ack);
    }

    private ProducerLatencyTimer latencyTimer(HermesMetrics hermesMetrics, TopicName topicName, Topic.Ack ack) {
        if (Topic.Ack.ALL.equals(ack)) {
            return new ProducerAckAllLatencyTimer(hermesMetrics, topicName);
        } else {
            return new ProducerAckLeaderLatencyTimer(hermesMetrics, topicName);
        }
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
        closeLatencyTimers();
        int responseStatus = ((HttpServletResponse) event.getSuppliedResponse()).getStatus();
        hermesMetrics.httpStatusCodeMeter(responseStatus).mark();
        hermesMetrics.httpStatusCodeMeter(responseStatus, topicName).mark();

        if (Family.SUCCESSFUL != Family.familyOf(responseStatus)) {
            hermesMetrics.meter(Meters.FAILED_METER).mark();
            hermesMetrics.meter(Meters.FAILED_TOPIC_METER, topicName).mark();
            hermesMetrics.counter(Counters.UNPUBLISHED, topicName).inc();
        }
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        closeLatencyTimers();
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }

    private void closeLatencyTimers() {
        producerLatencyTimer.close();
    }
}
