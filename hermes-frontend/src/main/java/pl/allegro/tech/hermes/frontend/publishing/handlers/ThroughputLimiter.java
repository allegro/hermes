package pl.allegro.tech.hermes.frontend.publishing.handlers;

import static java.lang.String.format;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.metrics.HermesRateMeter;

public interface ThroughputLimiter {
  QuotaInsight checkQuota(TopicName topic, HermesRateMeter throughput);

  default void start() {}

  default void stop() {}

  class QuotaInsight {
    private static final QuotaInsight QUOTA_CONFIRMED = new QuotaInsight();
    private static final QuotaInsight GLOBAL_VIOLATION =
        new QuotaInsight(false, "Global throughput exceeded. Sorry for the inconvenience.");
    private static final String DEFAULT_REASON = "unknown";

    private boolean hasQuota = true;
    private String reason;

    private QuotaInsight() {}

    private QuotaInsight(boolean pass, String reason) {
      this.hasQuota = pass;
      this.reason = reason;
    }

    public boolean hasQuota() {
      return hasQuota;
    }

    public String getReason() {
      return reason != null ? reason : DEFAULT_REASON;
    }

    public static QuotaInsight quotaConfirmed() {
      return QUOTA_CONFIRMED;
    }

    public static QuotaInsight quotaViolation(long current, long limit) {
      return new QuotaInsight(
          false,
          format("Current throughput exceeded limit [current:%s, limit:%s].", current, limit));
    }

    public static QuotaInsight globalQuotaViolation() {
      return GLOBAL_VIOLATION;
    }
  }
}
