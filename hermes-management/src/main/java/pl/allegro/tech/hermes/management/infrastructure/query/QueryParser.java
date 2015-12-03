package pl.allegro.tech.hermes.management.infrastructure.query;

public interface QueryParser {

    <T> Query<T> parse(String query, Class<T> type);
}
