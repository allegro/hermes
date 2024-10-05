package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.AND;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.EQ;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.GREATER_THAN;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.IN;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.LIKE;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.LOWER_THAN;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.NE;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.NOT;
import static pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator.OR;

import java.util.EnumMap;
import java.util.Map;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.Operator;

public class MatcherFactories {

  private static final Map<Operator, MatcherFactory> FACTORIES = new EnumMap<>(Operator.class);

  static {
    registerFactories();
  }

  public static MatcherFactory getMatcherFactory(String operator) {
    try {
      return getMatcherFactory(Operator.from(operator));
    } catch (IllegalArgumentException e) {
      throw new MatcherNotFoundException(String.format("Unrecognized operator: '%s'", operator));
    }
  }

  private static MatcherFactory getMatcherFactory(Operator operator) {
    return FACTORIES.get(operator);
  }

  public static MatcherFactory defaultMatcher() {
    return getMatcherFactory(EQ);
  }

  private static void registerFactories() {
    FACTORIES.put(EQ, (path, node, parser) -> new EqualityMatcher(path, parser.parseValue(node)));
    FACTORIES.put(
        NE,
        (path, node, parser) -> new NotMatcher(new EqualityMatcher(path, parser.parseValue(node))));
    FACTORIES.put(LIKE, (path, node, parser) -> new LikeMatcher(path, parser.parseValue(node)));
    FACTORIES.put(IN, (path, node, parser) -> new InMatcher(path, parser.parseArrayValue(node)));
    FACTORIES.put(NOT, (path, node, parser) -> new NotMatcher(parser.parseNode(node)));
    FACTORIES.put(AND, (path, node, parser) -> new AndMatcher(parser.parseArrayNodes(node)));
    FACTORIES.put(OR, (path, node, parser) -> new OrMatcher(parser.parseArrayNodes(node)));
    FACTORIES.put(
        GREATER_THAN,
        (path, node, parser) ->
            new ComparisonMatcher(path, parser.parseValue(node), (a, b) -> a > b));
    FACTORIES.put(
        LOWER_THAN,
        (path, node, parser) ->
            new ComparisonMatcher(path, parser.parseValue(node), (a, b) -> a < b));
  }
}
