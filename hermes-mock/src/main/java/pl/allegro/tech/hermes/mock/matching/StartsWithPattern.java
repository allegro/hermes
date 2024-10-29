package pl.allegro.tech.hermes.mock.matching;

import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import wiremock.com.fasterxml.jackson.annotation.JsonProperty;

public class StartsWithPattern extends StringValuePattern {

  public StartsWithPattern(@JsonProperty("startsWith") String expectedValue) {
    super(expectedValue);
  }

  public String getStartsWith() {
    return expectedValue;
  }

  @Override
  public MatchResult match(String value) {
    return MatchResult.of(value != null && value.startsWith(expectedValue));
  }
}
