package pl.allegro.tech.hermes.tracker.elasticsearch;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public abstract class DailyIndexFactory implements IndexFactory {

  private final String basePath;
  private final Clock clock;
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");

  public DailyIndexFactory(String basePath) {
    this(basePath, Clock.systemUTC());
  }

  public DailyIndexFactory(String basePath, Clock clock) {
    this.basePath = basePath;
    this.clock = clock;
  }

  @Override
  public String createIndex() {
    return basePath + "_" + dateTimeFormatter.format(LocalDate.now(clock));
  }
}
