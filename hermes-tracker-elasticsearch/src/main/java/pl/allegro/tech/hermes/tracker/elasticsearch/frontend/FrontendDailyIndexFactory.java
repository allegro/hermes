package pl.allegro.tech.hermes.tracker.elasticsearch.frontend;

import pl.allegro.tech.hermes.tracker.elasticsearch.DailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;

import java.time.Clock;

public class FrontendDailyIndexFactory extends DailyIndexFactory implements FrontendIndexFactory {

    public FrontendDailyIndexFactory(Clock clock) {
        super(SchemaManager.PUBLISHED_INDEX, clock);
    }

    public FrontendDailyIndexFactory() {
        super(SchemaManager.PUBLISHED_INDEX);
    }
}
