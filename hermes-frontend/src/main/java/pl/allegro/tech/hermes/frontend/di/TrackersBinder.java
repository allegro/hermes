package pl.allegro.tech.hermes.frontend.di;

import com.google.common.collect.ImmutableList;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import pl.allegro.tech.hermes.message.tracker.frontend.LogRepository;
import pl.allegro.tech.hermes.message.tracker.frontend.Trackers;

import java.util.List;

public class TrackersBinder extends AbstractBinder {

    private final List<LogRepository> logRepositories;

    public TrackersBinder() {
        this(ImmutableList.of());
    }

    public TrackersBinder(List<LogRepository> logRepositories) {
        this.logRepositories = logRepositories;
    }

    @Override
    protected void configure() {
        bind(new Trackers(logRepositories)).to(Trackers.class);
    }
}
