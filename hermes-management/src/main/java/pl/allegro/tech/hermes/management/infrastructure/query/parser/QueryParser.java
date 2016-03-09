package pl.allegro.tech.hermes.management.infrastructure.query.parser;

import pl.allegro.tech.hermes.api.Query;

import java.io.InputStream;

public interface QueryParser {

    <T> Query<T> parse(InputStream input, Class<T> type);

    <T> Query<T> parse(String query, Class<T> type);
}
