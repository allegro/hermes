package pl.allegro.tech.hermes.frontend.publishing;

import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Metrics;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.ws.rs.core.Response.Status.Family;

class MetricsAsyncListener implements AsyncListener {

    private HermesMetrics hermesMetrics;
    private TopicName topicName;
    private Timer.Context latencyTimer;
    private Timer.Context latencyTimerPerTopic;

    MetricsAsyncListener(HermesMetrics hermesMetrics, TopicName topicName) {
        this.hermesMetrics = hermesMetrics;
        this.topicName = topicName;
        initLatencyTimers();
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
        closeLatencyTimers();
        int responseStatus = ((HttpServletResponse) event.getSuppliedResponse()).getStatus();
        hermesMetrics.httpStatusCodeMeter(responseStatus).mark();
        hermesMetrics.httpStatusCodeMeter(responseStatus, topicName).mark();

        if (Family.SUCCESSFUL != Family.familyOf(responseStatus)) {
            hermesMetrics.meter(Metrics.Meter.PRODUCER_FAILED_METER).mark();
            hermesMetrics.meter(Metrics.Meter.PRODUCER_FAILED_METER, topicName).mark();
            hermesMetrics.counter(Metrics.Counter.PRODUCER_UNPUBLISHED, topicName).inc();
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
        hermesMetrics.close(latencyTimer, latencyTimerPerTopic);
    }

    private void initLatencyTimers() {
        this.latencyTimer = hermesMetrics.timer(Metrics.Timer.PRODUCER_LATENCY).time();
        this.latencyTimerPerTopic = hermesMetrics.timer(Metrics.Timer.PRODUCER_LATENCY, topicName).time();
    }

}
