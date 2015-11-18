package pl.allegro.tech.hermes.integration.test;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.assertj.core.api.AbstractAssert;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class WiremockRequestAssertion extends AbstractAssert<WiremockRequestAssertion, LoggedRequest> {

    protected WiremockRequestAssertion(LoggedRequest actual) {
        super(actual, WiremockRequestAssertion.class);
    }

    public WiremockRequestAssertion hasHeaderValue(String header, String value) {
        assertThat(actual.getHeader(header)).isEqualTo(value);
        return this;
    }

    public WiremockRequestAssertion containsAllHeaders(Map<String, String> headers) {
        headers.forEach((header, value) -> {
            assertThat(actual.getHeader(header)).isEqualTo(value);
        });
        return this;
    }
}
