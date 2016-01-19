package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import com.fasterxml.jackson.databind.JsonNode;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.QueryParserContext;

public interface MatcherFactory {

    Matcher createMatcher(String path, JsonNode value, QueryParserContext context);
}
