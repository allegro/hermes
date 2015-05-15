package pl.allegro.tech.hermes.integration.helper.graphite;

public class MetricCreator {

    private static final String SEPARATOR = " ";

    public static Metric create(String data) {
        String [] values = data.split(SEPARATOR);

        return new Metric(
            values[0],
            Double.valueOf(values[1]),
            Integer.valueOf(values[2])
        );
    }
}
