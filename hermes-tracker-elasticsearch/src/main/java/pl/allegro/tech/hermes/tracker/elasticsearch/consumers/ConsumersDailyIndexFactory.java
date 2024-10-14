package pl.allegro.tech.hermes.tracker.elasticsearch.consumers;

import java.time.Clock;
import pl.allegro.tech.hermes.tracker.elasticsearch.DailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;

public class ConsumersDailyIndexFactory extends DailyIndexFactory implements ConsumersIndexFactory {

  public ConsumersDailyIndexFactory(Clock clock) {
    super(SchemaManager.SENT_INDEX, clock);
  }

  public ConsumersDailyIndexFactory() {
    super(SchemaManager.SENT_INDEX);
  }
}
