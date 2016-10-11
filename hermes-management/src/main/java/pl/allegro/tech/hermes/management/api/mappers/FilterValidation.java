package pl.allegro.tech.hermes.management.api.mappers;

public class FilterValidation {

    public boolean isFiltered() {
        return isFiltered;
    }

    private final boolean isFiltered;

    public FilterValidation(boolean isFiltered) {
        this.isFiltered = isFiltered;
    }
}
