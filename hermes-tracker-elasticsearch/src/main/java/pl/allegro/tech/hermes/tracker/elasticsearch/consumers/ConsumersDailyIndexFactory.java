package pl.allegro.tech.hermes.tracker.elasticsearch.consumers;

import pl.allegro.tech.hermes.tracker.elasticsearch.DailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;

import java.time.Clock;

public class ConsumersDailyIndexFactory extends DailyIndexFactory implements ConsumersIndexFactory {

    public ConsumersDailyIndexFactory(Clock clock) {
        super(SchemaManager.SENT_INDEX, clock);
    }

    public ConsumersDailyIndexFactory() {
        super(SchemaManager.SENT_INDEX);
    }
}
