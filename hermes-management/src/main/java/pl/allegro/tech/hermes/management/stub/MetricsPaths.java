package pl.allegro.tech.hermes.management.stub;

public class MetricsPaths {

    public static final String REPLACEMENT_CHAR = "_";

    private final String prefix;

    public MetricsPaths(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }

    public static String escapeDots(String value) {
        return value.replaceAll("\\.", REPLACEMENT_CHAR);
    }

}
