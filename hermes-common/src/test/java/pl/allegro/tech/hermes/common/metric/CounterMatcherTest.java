package pl.allegro.tech.hermes.common.metric;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import pl.allegro.tech.hermes.common.metric.counter.CounterMatcher;

import static junitparams.JUnitParamsRunner.$;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.common.metric.Metrics.Counter.CONSUMER_DELIVERED;
import static pl.allegro.tech.hermes.common.metric.Metrics.Counter.CONSUMER_DISCARDED;
import static pl.allegro.tech.hermes.common.metric.Metrics.Counter.CONSUMER_INFLIGHT;
import static pl.allegro.tech.hermes.common.metric.Metrics.Counter.PRODUCER_PUBLISHED;
import static pl.allegro.tech.hermes.common.metric.Metrics.Counter.PRODUCER_UNPUBLISHED;

@RunWith(JUnitParamsRunner.class)
public class CounterMatcherTest {

    @Test
    @Parameters(method = "counterProvider")
    public void shouldMatchTopicPublishedCounter(String prefix, String suffix,
                                                 boolean expectedMatch, boolean expectedTopic,
                                                 Metrics.Counter expectedCounter,
                                                 String expectedTopicName, String expectedSubscriptionName) {
        CounterMatcher matcher = new CounterMatcher("tech.hermes", prefix + "." + Metrics.ESCAPED_HOSTNAME + "." + suffix);

        assertThat(matcher.matches()).isEqualTo(expectedMatch);
        assertThat(matcher.isTopic()).isEqualTo(expectedTopic);
        assertThat(matcher.getCounter()).isEqualTo(expectedCounter);
        assertThat(matcher.getTopicName()).isEqualTo(expectedTopicName);
        assertThat(matcher.getSubscriptionName()).isEqualTo(expectedSubscriptionName);
    }

    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private Object[] counterProvider() {
        return $(
            $("tech.hermes.producer", "published.group1.topic1",
                    true, true, PRODUCER_PUBLISHED, "group1.topic1", null),
            $("tech.hermes.producer", "unpublished.group1.topic1",
                    true, true, PRODUCER_UNPUBLISHED, "group1.topic1", null),
            $("tech.hermes.consumer", "delivered.group1.topic1.subscription1",
                    true, false, CONSUMER_DELIVERED, "group1.topic1", "subscription1"),
            $("tech.hermes.consumer", "discarded.group1.topic1.subscription1",
                    true, false, CONSUMER_DISCARDED, "group1.topic1", "subscription1"),
            $("tech.hermes.consumer", "inflight.group1.topic1.subscription1",
                    true, false, CONSUMER_INFLIGHT, "group1.topic1", "subscription1")
        );
    }

}
