package pl.allegro.tech.hermes.consumers.consumer.filtering;

import java.util.Optional;

public class FilterResult {

    public static final FilterResult PASS = new FilterResult(true, Optional.empty());

    public static FilterResult failed(final String filterType) {
        return new FilterResult(false, Optional.of(filterType));
    }

    public final boolean filtered;
    public final Optional<String> filterType;

    private FilterResult(final boolean filtered, final Optional<String> filterType) {
        this.filtered = filtered;
        this.filterType = filterType;
    }
}
