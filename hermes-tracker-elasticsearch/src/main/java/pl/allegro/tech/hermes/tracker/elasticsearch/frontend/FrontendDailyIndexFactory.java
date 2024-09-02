package pl.allegro.tech.hermes.tracker.elasticsearch.frontend;

import java.time.Clock;
import pl.allegro.tech.hermes.tracker.elasticsearch.DailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;

public class FrontendDailyIndexFactory extends DailyIndexFactory implements FrontendIndexFactory {

  public FrontendDailyIndexFactory(Clock clock) {
    super(SchemaManager.PUBLISHED_INDEX, clock);
  }

  public FrontendDailyIndexFactory() {
    super(SchemaManager.PUBLISHED_INDEX);
  }
}
