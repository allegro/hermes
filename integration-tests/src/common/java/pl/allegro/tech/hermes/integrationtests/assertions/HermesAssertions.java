package pl.allegro.tech.hermes.integrationtests.assertions;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.pubsub.v1.PubsubMessage;
import jakarta.ws.rs.core.Response;
import org.assertj.core.api.Assertions;

public final class HermesAssertions extends Assertions {

  private HermesAssertions() {}

  public static HttpResponseAssertion assertThat(Response response) {
    return new HttpResponseAssertion(response);
  }

  public static WiremockRequestAssertion assertThat(LoggedRequest request) {
    return new WiremockRequestAssertion(request);
  }

  public static PrometheusMetricsAssertion assertThatMetrics(String body) {
    return new PrometheusMetricsAssertion(body);
  }

  public static GooglePubSubAssertion assertThat(PubsubMessage message) {
    return new GooglePubSubAssertion(message);
  }
}
