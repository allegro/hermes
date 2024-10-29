package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.jxpath.JXPathException;
import pl.allegro.tech.hermes.management.infrastructure.query.graph.ObjectGraph;

public class LikeMatcher implements Matcher {

  private final String attribute;

  private final Pattern expected;

  public LikeMatcher(String attribute, Object expected) {
    this.attribute = attribute;
    try {
      this.expected = Pattern.compile(asString(expected));
    } catch (PatternSyntaxException e) {
      throw new MatcherException(
          String.format("Could not parse regexp pattern: '%s'", expected), e);
    }
  }

  @Override
  public boolean match(Object value) {
    try {
      Object actual = ObjectGraph.from(value).navigate(attribute).value();
      return expected.matcher(asString(actual)).matches();
    } catch (JXPathException e) {
      throw new MatcherException(
          String.format("Could not navigate to specific path: '%s'", attribute), e);
    }
  }

  private static String asString(Object value) {
    return String.valueOf(value);
  }
}
