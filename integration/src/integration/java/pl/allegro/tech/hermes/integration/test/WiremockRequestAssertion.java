package pl.allegro.tech.hermes.integration.test;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.assertj.core.api.AbstractAssert;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class WireMockRequestAssertion extends AbstractAssert<WireMockRequestAssertion, LoggedRequest> {

    protected WireMockRequestAssertion(LoggedRequest actual) {
        super(actual, WireMockRequestAssertion.class);
    }

    public WireMockRequestAssertion hasHeaderValue(String header, String value) {
        assertThat(actual.getHeader(header)).isEqualTo(value);
        return this;
    }

    public WireMockRequestAssertion containsAllHeaders(Map<String, String> headers) {
        headers.forEach((header, value) -> {
            assertThat(actual.getHeader(header)).isEqualTo(value);
        });
        return this;
    }
}
