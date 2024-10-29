package pl.allegro.tech.hermes.infrastructure.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * This class is an implementation of {@link TurboFilter} interface that allows messages of
 * configured loggers to be aggregated and logged only once with specified time interval.
 *
 * <p>Logged message contains a suffix "[occurrences=N]" with a number of logged events. The logging
 * part is a little bit tricky, as the <code>TurboFilter</code> interface does not support a way of
 * logging out of the box, it just tells whether a logged message should be passed further or not.
 * In order to logging be possible from the <code>TurboFilter</code> a small trick has to be
 * applied, the logged message is enriched with a custom {@link Marker} which is checked by the
 * filter itself, so it won't filter it's own messages and we manage to avoid recursion.
 *
 * <p>An instance of <code>AggregatingTurboFilter</code> starts it's own scheduled executor service
 * with a single thread that logs the messages asynchronously.
 *
 * <p>Example logback configuration:
 *
 * <pre>{@code
 *  <configuration>
 *      ...
 *      <turboFilter class="pl.allegro.tech.hermes.infrastructure.logback.AggregatingTurboFilter">
 *          <reportingIntervalMillis>10000</reportingIntervalMillis>
 *          <aggregatedLogger>org.apache.zookeeper.ClientCnxn</aggregatedLogger>
 *          <aggregatedLogger>pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper</aggregatedLogger>
 *          ...
 *      </turboFilter>
 *      ...
 * </configuration>
 * }</pre>
 */
public class AggregatingTurboFilter extends TurboFilter {

  static final Marker MARKER = MarkerFactory.getMarker("AggregatingTurboFilterMarker");

  private ScheduledExecutorService executorService;
  private final List<String> aggregatedLogger = new ArrayList<>();
  private long reportingIntervalMillis = 10_000;

  private final Map<Logger, LoggerAggregates> logAggregates = new ConcurrentHashMap<>();

  private static final LongAdder filterClassCounter = new LongAdder();

  @Override
  public void start() {
    super.start();
    if (!aggregatedLogger.isEmpty()) {
      ThreadFactory threadFactory =
          new ThreadFactoryBuilder()
              .setNameFormat("aggregating-filter-" + filterClassCounter.longValue() + "-thread-%d")
              .build();
      filterClassCounter.increment();
      executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
      executorService.scheduleAtFixedRate(
          this::report, 0L, getReportingIntervalMillis(), TimeUnit.MILLISECONDS);
    }
  }

  @Override
  public void stop() {
    executorService.shutdownNow();
    super.stop();
  }

  void report() {
    logAggregates.forEach(
        (logger, loggerAggregates) ->
            loggerAggregates
                .aggregates
                .keySet()
                .forEach(
                    entry -> {
                      loggerAggregates.aggregates.computeIfPresent(
                          entry,
                          (key, summary) -> {
                            logger.log(
                                key.marker,
                                Logger.FQCN,
                                key.level,
                                key.message + " [occurrences=" + summary.logsCount + "]",
                                key.params,
                                summary.lastException);
                            return null;
                          });
                    }));
  }

  @Override
  public FilterReply decide(
      Marker marker, Logger logger, Level level, String message, Object[] params, Throwable ex) {
    if (isAggregatedLog(marker)) { // prevent recursion for aggregated logger events
      return FilterReply.NEUTRAL;
    }
    if (!aggregatedLogger.contains(logger.getName())) {
      return FilterReply.NEUTRAL;
    }

    if (ex == null) {
      Optional<Throwable> throwable = extractLastParamThrowable(params);
      if (throwable.isPresent()) {
        ex = throwable.get();
        params = Arrays.copyOfRange(params, 0, params.length - 1);
      }
    }
    Throwable exception = ex;

    LoggingEventKey loggingEventKey =
        new LoggingEventKey(message, params, level, getEnrichedMarker(marker));
    logAggregates
        .computeIfAbsent(logger, l -> new LoggerAggregates())
        .aggregates
        .merge(
            loggingEventKey,
            new AggregateSummary(ex),
            (currentAggregate, emptyAggregate) ->
                AggregateSummary.incrementCount(currentAggregate, exception));

    return FilterReply.DENY;
  }

  private boolean isAggregatedLog(Marker marker) {
    return marker != null && (marker.equals(MARKER) || marker.contains(MARKER));
  }

  private Optional<Throwable> extractLastParamThrowable(Object[] params) {
    return Optional.ofNullable(params)
        .map(Arrays::stream)
        .flatMap(
            a ->
                a.skip(params.length - 1)
                    .findFirst()
                    .filter(o -> o instanceof Throwable)
                    .map(Throwable.class::cast));
  }

  private Marker getEnrichedMarker(Marker marker) {
    if (marker == null) {
      return MARKER;
    }
    marker.add(MARKER);
    return marker;
  }

  public void addAggregatedLogger(String logger) {
    this.aggregatedLogger.add(logger);
  }

  public long getReportingIntervalMillis() {
    return reportingIntervalMillis;
  }

  public void setReportingIntervalMillis(long reportingIntervalMillis) {
    this.reportingIntervalMillis = reportingIntervalMillis;
  }

  private static class LoggerAggregates {

    private final Map<LoggingEventKey, AggregateSummary> aggregates = new ConcurrentHashMap<>();
  }

  private static class AggregateSummary {

    private final int logsCount;
    private final Throwable lastException;

    private AggregateSummary(Throwable lastException) {
      this(1, lastException);
    }

    private AggregateSummary(int logsCount, Throwable lastException) {
      this.logsCount = logsCount;
      this.lastException = lastException;
    }

    private static AggregateSummary incrementCount(
        AggregateSummary currentAggregate, Throwable lastException) {
      return new AggregateSummary(
          currentAggregate.logsCount + 1,
          Optional.ofNullable(lastException).orElse(currentAggregate.lastException));
    }
  }

  private static class LoggingEventKey {

    private final String message;
    private final int level;
    private final Marker marker;
    private final Object[] params;

    LoggingEventKey(String message, Object[] params, Level level, Marker marker) {
      this.message = message;
      this.params = params;
      this.level = Level.toLocationAwareLoggerInteger(level);
      this.marker = marker;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LoggingEventKey that = (LoggingEventKey) o;
      return level == that.level
          && Objects.equals(message, that.message)
          && Objects.equals(marker, that.marker)
          && Arrays.equals(params, that.params);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(message, level, marker);
      result = 31 * result + Arrays.hashCode(params);
      return result;
    }
  }
}
