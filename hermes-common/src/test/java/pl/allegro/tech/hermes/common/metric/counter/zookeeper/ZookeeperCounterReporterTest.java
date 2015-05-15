package pl.allegro.tech.hermes.common.metric.counter.zookeeper;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;

import java.util.SortedMap;
import java.util.TreeMap;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.common.metric.Metrics.Counter.CONSUMER_DELIVERED;
import static pl.allegro.tech.hermes.common.metric.Metrics.Counter.CONSUMER_DISCARDED;
import static pl.allegro.tech.hermes.common.metric.Metrics.Counter.PRODUCER_PUBLISHED;

@RunWith(MockitoJUnitRunner.class)
public class ZookeeperCounterReporterTest {

    public static final SortedMap<String, Timer> EMPTY_TIMERS = null;
    public static final SortedMap<String, Meter> EMPTY_METERS = null;
    public static final SortedMap<String, Histogram> EMPTY_HISTOGRAMS = null;
    public static final SortedMap<String, Gauge> EMPTY_GAUGES = null;
    public static final String GROUP_NAME_UNDERSCORE = "my_group";
    public static final String GROUP_NAME = "my.group";
    public static final String TOPIC_NAME = "topic1";
    public static final String SUBSCRIPTION_NAME_UNDERSCORE = "subscription_name";
    public static final String SUBSCRIPTION_NAME = "subscription.name";
    public static final TopicName QUALIFIED_TOPIC_NAME = new TopicName(GROUP_NAME, TOPIC_NAME);
    public static final long COUNT = 100L;
    public static final String GRAPHITE_PREFIX = "tech.hermes";
    public static final String METRIC_NAME_FOR_PUBLISHED =
        PRODUCER_PUBLISHED.displayName(GRAPHITE_PREFIX) + "." + GROUP_NAME_UNDERSCORE + "." + TOPIC_NAME;
    public static final String METRIC_NAME_FOR_DELIVERED = CONSUMER_DELIVERED.displayName(GRAPHITE_PREFIX) + "."
                + GROUP_NAME_UNDERSCORE + "." + TOPIC_NAME + "." + SUBSCRIPTION_NAME_UNDERSCORE;
    public static final String METRIC_NAME_FOR_DISCARDED =
        CONSUMER_DISCARDED.displayName(GRAPHITE_PREFIX) + "." + GROUP_NAME_UNDERSCORE + "." + TOPIC_NAME
                + "." + SUBSCRIPTION_NAME_UNDERSCORE;

    @Mock
    private CounterStorage counterStorage;

    @Mock
    private MetricRegistry metricRegistry;

    @Mock
    private Counter counter;

    @Mock
    private ConfigFactory configFactory;

    private ZookeeperCounterReporter zookeeperCounterReporter;

    @Before
    public void before() {
        when(configFactory.getStringProperty(Configs.GRAPHITE_PREFIX)).thenReturn(GRAPHITE_PREFIX);
        zookeeperCounterReporter = new ZookeeperCounterReporter(metricRegistry, counterStorage, configFactory);
    }

    @Test
    public void shouldReportPublishedMessages() {
        // given
        SortedMap<String, Counter> counters = prepareCounters(METRIC_NAME_FOR_PUBLISHED);
        when(counter.getCount()).thenReturn(COUNT);

        // when
        zookeeperCounterReporter.report(EMPTY_GAUGES, counters, EMPTY_HISTOGRAMS, EMPTY_METERS, EMPTY_TIMERS);

        // then
        verify(counterStorage).setTopicCounter(QUALIFIED_TOPIC_NAME, PRODUCER_PUBLISHED, COUNT);
    }

    @Test
    public void shouldReportDeliveredMessages() {
        SortedMap<String, Counter> counters = prepareCounters(METRIC_NAME_FOR_DELIVERED);
        when(counter.getCount()).thenReturn(COUNT);

        zookeeperCounterReporter.report(EMPTY_GAUGES, counters, EMPTY_HISTOGRAMS, EMPTY_METERS, EMPTY_TIMERS);

        verify(counterStorage).setSubscriptionCounter(QUALIFIED_TOPIC_NAME, SUBSCRIPTION_NAME, CONSUMER_DELIVERED, COUNT);
    }

    @Test
    public void shouldReportDiscardedMessages() {
        SortedMap<String, Counter> counters = prepareCounters(METRIC_NAME_FOR_DISCARDED);
        when(counter.getCount()).thenReturn(COUNT);

        zookeeperCounterReporter.report(EMPTY_GAUGES, counters, EMPTY_HISTOGRAMS, EMPTY_METERS, EMPTY_TIMERS);

        verify(counterStorage).setSubscriptionCounter(
                QUALIFIED_TOPIC_NAME, SUBSCRIPTION_NAME, CONSUMER_DISCARDED, COUNT
        );
    }

    private SortedMap<String, Counter> prepareCounters(String metricName) {
        SortedMap<String, Counter> counters = new TreeMap<>();
        counters.put(metricName, counter);

        return counters;
    }

}
