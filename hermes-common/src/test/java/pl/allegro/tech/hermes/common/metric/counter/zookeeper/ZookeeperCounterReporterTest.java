package pl.allegro.tech.hermes.common.metric.counter.zookeeper;

import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.util.InstanceIdResolver;

@RunWith(MockitoJUnitRunner.class)
public class ZookeeperCounterReporterTest {
  public static final String GROUP_NAME = "pl.allegro.tech.skylab";
  public static final String TOPIC_NAME_UNDERSCORE = "topic_1";
  public static final String SUBSCRIPTION_NAME = "subscription.name";
  public static final TopicName topic = new TopicName(GROUP_NAME, TOPIC_NAME_UNDERSCORE);
  public static final SubscriptionName subscription =
      new SubscriptionName(SUBSCRIPTION_NAME, topic);

  public static final long COUNT = 100L;

  @Mock private CounterStorage counterStorage;

  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  private final MetricsFacade metricsFacade = new MetricsFacade(meterRegistry);

  @Mock private InstanceIdResolver instanceIdResolver;

  private ZookeeperCounterReporter zookeeperCounterReporter;

  @Before
  public void before() {
    zookeeperCounterReporter = new ZookeeperCounterReporter(meterRegistry, counterStorage, "");
  }

  @Test
  public void shouldReportPublishedMessages() {
    // given
    metricsFacade.topics().topicPublished(topic, "dc1").increment(COUNT);

    // when
    zookeeperCounterReporter.report();

    // then
    verify(counterStorage).setTopicPublishedCounter(topic, COUNT);
  }

  @Test
  public void shouldReportDeliveredMessages() {
    // given
    metricsFacade.subscriptions().successes(subscription).increment(COUNT);

    // when
    zookeeperCounterReporter.report();

    // then
    verify(counterStorage).setSubscriptionDeliveredCounter(topic, SUBSCRIPTION_NAME, COUNT);
  }

  @Test
  public void shouldReportDiscardedMessages() {
    // given
    metricsFacade.subscriptions().discarded(subscription).increment(COUNT);

    // when
    zookeeperCounterReporter.report();

    // then
    verify(counterStorage).setSubscriptionDiscardedCounter(topic, SUBSCRIPTION_NAME, COUNT);
  }

  @Test
  public void shouldReportSubscriptionVolumeCounter() {
    // given
    metricsFacade.subscriptions().throughputInBytes(subscription).increment(COUNT);

    // when
    zookeeperCounterReporter.report();

    // then
    verify(counterStorage).incrementVolumeCounter(topic, SUBSCRIPTION_NAME, COUNT);
  }

  @Test
  public void shouldReportTopicVolumeCounter() {
    // given
    metricsFacade.topics().topicThroughputBytes(topic).increment(COUNT);

    // when
    zookeeperCounterReporter.report();

    // then
    verify(counterStorage).incrementVolumeCounter(topic, COUNT);
  }
}
