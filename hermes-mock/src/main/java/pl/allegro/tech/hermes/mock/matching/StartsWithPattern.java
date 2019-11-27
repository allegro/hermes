package pl.allegro.tech.hermes.mock.matching;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

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
