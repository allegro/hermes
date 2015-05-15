package pl.allegro.tech.hermes.management.infrastructure.graphite;

public class GraphiteConnectionException extends RuntimeException {

    public GraphiteConnectionException(Throwable cause) {
        super("Error connecting to Graphite", cause);
    }

}
