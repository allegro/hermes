package pl.allegro.tech.hermes.integrationtests.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.Map;
import org.assertj.core.api.AbstractAssert;

public class WiremockRequestAssertion
    extends AbstractAssert<WiremockRequestAssertion, LoggedRequest> {

  protected WiremockRequestAssertion(LoggedRequest actual) {
    super(actual, WiremockRequestAssertion.class);
  }

  public WiremockRequestAssertion hasHeaderValue(String header, String value) {
    assertThat(actual.getHeader(header)).isEqualTo(value);
    return this;
  }

  public WiremockRequestAssertion containsAllHeaders(Map<String, String> headers) {
    headers.forEach((header, value) -> assertThat(actual.getHeader(header)).isEqualTo(value));
    return this;
  }
}
