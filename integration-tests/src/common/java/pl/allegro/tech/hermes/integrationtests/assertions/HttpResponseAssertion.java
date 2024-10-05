package pl.allegro.tech.hermes.integrationtests.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;
import org.assertj.core.api.AbstractAssert;

public class HttpResponseAssertion extends AbstractAssert<HttpResponseAssertion, Response> {

  HttpResponseAssertion(Response actual) {
    super(actual, HttpResponseAssertion.class);
  }

  public HttpResponseAssertion hasStatus(Response.Status status) {
    assertThat(actual.getStatus()).isEqualTo(status.getStatusCode());
    return this;
  }
}
