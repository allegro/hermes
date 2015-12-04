package pl.allegro.tech.hermes.management.infrastructure.query.parser;

import pl.allegro.tech.hermes.common.query.Query;

public interface QueryParser {

    <T> Query<T> parse(String query, Class<T> type);
}
