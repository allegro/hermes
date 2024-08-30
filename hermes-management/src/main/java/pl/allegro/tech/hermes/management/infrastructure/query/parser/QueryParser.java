package pl.allegro.tech.hermes.management.infrastructure.query.parser;

import java.io.InputStream;
import pl.allegro.tech.hermes.api.Query;

public interface QueryParser {

  <T> Query<T> parse(InputStream input, Class<T> type);

  <T> Query<T> parse(String query, Class<T> type);
}
