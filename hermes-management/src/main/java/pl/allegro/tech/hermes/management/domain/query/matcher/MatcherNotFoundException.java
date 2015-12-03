package pl.allegro.tech.hermes.management.domain.query.matcher;

public class MatcherNotFoundException extends RuntimeException {

    public MatcherNotFoundException(String message) {
        super(message);
    }

    public MatcherNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
