package pl.allegro.tech.hermes.management.infrastructure.query.parser;

import com.fasterxml.jackson.databind.JsonNode;
import pl.allegro.tech.hermes.management.infrastructure.query.matcher.Matcher;

import java.util.List;

public interface QueryParserContext {

    <T> Matcher<T> parseNode(JsonNode node);

    <T> List<Matcher<T>> parseNodes(JsonNode node);

    Object parseValue(JsonNode node);

    Object[] parseArrayValue(JsonNode node);
}
