package pl.allegro.tech.hermes.common.metric.counter;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import pl.allegro.tech.hermes.common.metric.Metrics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CounterMatcher {

    private static final int SOURCE_NAME_INDEX = 1;
    private static final int HOSTNAME_INDEX = 2;
    private static final int NORMALIZE_NAME_INDEX = 3;
    private static final int TOPIC_NAME_INDEX = 4;
    private static final int SUBSCRIPTION_NAME_INDEX = 7;

    public static final String DOT = "\\.";
    public static final String SOURCE_NAME = "([^\\.]+)";
    public static final String HOSTNAME = String.format("(\\Q%s\\E)", Metrics.ESCAPED_HOSTNAME);
    public static final String NORMALIZE_NAME = "([^\\.]+)";
    public static final String QUALIFIED_TOPIC_NAME = "([^\\.]+\\.([^\\.]+))";
    public static final String OPTIONAL_SUBSCRIPTION_NAME = "(\\.?([^\\.]+))?";

    private final Matcher matcher;

    public CounterMatcher(String graphitePrefix, String counterName) {
        this.matcher = createMatcher(graphitePrefix, counterName);
    }

    private Matcher createMatcher(String prefix, String counterName) {
        String[] tokens = {prefix, SOURCE_NAME, HOSTNAME, NORMALIZE_NAME, QUALIFIED_TOPIC_NAME + OPTIONAL_SUBSCRIPTION_NAME};
        return Pattern.compile(Joiner.on(DOT).join(tokens)).matcher(counterName);
    }

    public boolean matches() {
        return matcher.matches();
    }

    public boolean isTopic() {
        return !isSubscription();
    }

    public boolean isSubscription() {
        return Optional.fromNullable(matcher.group(SUBSCRIPTION_NAME_INDEX)).isPresent();
    }

    public boolean isInflight() {
        return matcher.group(NORMALIZE_NAME_INDEX).equals(Metrics.Counter.CONSUMER_INFLIGHT.normalizedName());
    }

    public Metrics.Counter getCounter() {
        return Metrics.Counter.lookup(matcher.group(SOURCE_NAME_INDEX), matcher.group(NORMALIZE_NAME_INDEX));
    }

    public String getTopicName() {
        return matcher.group(TOPIC_NAME_INDEX);
    }

    public String getSubscriptionName() {
        return matcher.group(SUBSCRIPTION_NAME_INDEX);
    }

    public String getHostname() {
        return matcher.group(HOSTNAME_INDEX);
    }
}
