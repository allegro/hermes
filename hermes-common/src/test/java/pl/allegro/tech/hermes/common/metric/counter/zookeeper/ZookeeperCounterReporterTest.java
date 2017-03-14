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
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.util.HostnameResolver;

import java.util.SortedMap;
import java.util.TreeMap;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.common.metric.Counters.DELIVERED;
import static pl.allegro.tech.hermes.common.metric.Counters.DISCARDED;
import static pl.allegro.tech.hermes.common.metric.Counters.PUBLISHED;
import static pl.allegro.tech.hermes.metrics.PathContext.pathContext;

@RunWith(MockitoJUnitRunner.class)
public class ZookeeperCounterReporterTest {

    public static final SortedMap<String, Timer> EMPTY_TIMERS = null;
    public static final SortedMap<String, Meter> EMPTY_METERS = null;
    public static final SortedMap<String, Histogram> EMPTY_HISTOGRAMS = null;
    public static final SortedMap<String, Gauge> EMPTY_GAUGES = null;
    public static final String GROUP_NAME_UNDERSCORE = "pl_allegro_tech_skylab";
    public static final String GROUP_NAME = "pl.allegro.tech.skylab";
    public static final String TOPIC_NAME_UNDERSCORE = "topic_1";
    public static final String SUBSCRIPTION_NAME_UNDERSCORE = "subscription_name";
    public static final String SUBSCRIPTION_NAME = "subscription.name";
    public static final TopicName QUALIFIED_TOPIC_NAME = new TopicName(GROUP_NAME, TOPIC_NAME_UNDERSCORE);
    public static final long COUNT = 100L;
    public static final String GRAPHITE_PREFIX = "tech.hermes";

    private static PathsCompiler pathsCompiler = new PathsCompiler("localhost.domain");

    public static final String METRIC_NAME_FOR_PUBLISHED = pathsCompiler.compile(PUBLISHED, pathContext()
            .withGroup(GROUP_NAME_UNDERSCORE).withTopic(TOPIC_NAME_UNDERSCORE).build());

    public static final String METRIC_NAME_FOR_DELIVERED = pathsCompiler.compile(DELIVERED, pathContext()
            .withGroup(GROUP_NAME_UNDERSCORE).withTopic(TOPIC_NAME_UNDERSCORE).withSubscription(SUBSCRIPTION_NAME_UNDERSCORE).build());

    public static final String METRIC_NAME_FOR_DISCARDED = pathsCompiler.compile(DISCARDED, pathContext()
            .withGroup(GROUP_NAME_UNDERSCORE).withTopic(TOPIC_NAME_UNDERSCORE).withSubscription(SUBSCRIPTION_NAME_UNDERSCORE).build());

    @Mock
    private CounterStorage counterStorage;

    @Mock
    private MetricRegistry metricRegistry;

    @Mock
    private Counter counter;

    @Mock
    private ConfigFactory configFactory;

    @Mock
    private HostnameResolver hostnameResolver;

    private ZookeeperCounterReporter zookeeperCounterReporter;



    @Before
    public void before() {
        when(configFactory.getStringProperty(Configs.GRAPHITE_PREFIX)).thenReturn(GRAPHITE_PREFIX);
        when(hostnameResolver.resolve()).thenReturn("localhost.domain");
        zookeeperCounterReporter = new ZookeeperCounterReporter(metricRegistry, counterStorage, configFactory);
    }

    @Test
    public void shouldReportPublishedMessages() {
        SortedMap<String, Counter> counters = prepareCounters(METRIC_NAME_FOR_PUBLISHED);
        when(counter.getCount()).thenReturn(COUNT);

        zookeeperCounterReporter.report(EMPTY_GAUGES, counters, EMPTY_HISTOGRAMS, EMPTY_METERS, EMPTY_TIMERS);

        verify(counterStorage).setTopicPublishedCounter(QUALIFIED_TOPIC_NAME, COUNT);
    }

    @Test
    public void shouldReportDeliveredMessages() {
        SortedMap<String, Counter> counters = prepareCounters(METRIC_NAME_FOR_DELIVERED);
        when(counter.getCount()).thenReturn(COUNT);

        zookeeperCounterReporter.report(EMPTY_GAUGES, counters, EMPTY_HISTOGRAMS, EMPTY_METERS, EMPTY_TIMERS);

        verify(counterStorage).setSubscriptionDeliveredCounter(QUALIFIED_TOPIC_NAME, SUBSCRIPTION_NAME, COUNT);
    }

    @Test
    public void shouldReportDiscardedMessages() {
        SortedMap<String, Counter> counters = prepareCounters(METRIC_NAME_FOR_DISCARDED);
        when(counter.getCount()).thenReturn(COUNT);

        zookeeperCounterReporter.report(EMPTY_GAUGES, counters, EMPTY_HISTOGRAMS, EMPTY_METERS, EMPTY_TIMERS);

        verify(counterStorage).setSubscriptionDiscardedCounter(
                QUALIFIED_TOPIC_NAME, SUBSCRIPTION_NAME, COUNT
        );
    }

    private SortedMap<String, Counter> prepareCounters(String metricName) {
        SortedMap<String, Counter> counters = new TreeMap<>();
        counters.put(metricName, counter);

        return counters;
    }

}