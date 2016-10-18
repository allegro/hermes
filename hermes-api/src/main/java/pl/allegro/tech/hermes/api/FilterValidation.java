package pl.allegro.tech.hermes.api;

import java.beans.ConstructorProperties;

public class FilterValidation {
    private final boolean isFiltered;

    @ConstructorProperties({"filtered"})
    public FilterValidation(boolean isFiltered) {
        this.isFiltered = isFiltered;
    }

    public boolean isFiltered() {
        return isFiltered;
    }
}
