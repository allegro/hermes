package pl.allegro.tech.hermes.mock.exchange;

import java.time.Duration;
import wiremock.org.apache.hc.core5.http.HttpStatus;

public class Response {
  private final int statusCode;
  private final Duration fixedDelay;

  public Response(int statusCode, Duration fixedDelay) {
    this.statusCode = statusCode;
    this.fixedDelay = fixedDelay;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Duration getFixedDelay() {
    return fixedDelay;
  }

  public static final class Builder {
    private int statusCode = HttpStatus.SC_CREATED;
    private Duration fixedDelay;

    private Builder() {}

    public static Builder aResponse() {
      return new Builder();
    }

    public Builder withStatusCode(int statusCode) {
      this.statusCode = statusCode;
      return this;
    }

    public Builder withFixedDelay(Duration fixedDelay) {
      this.fixedDelay = fixedDelay;
      return this;
    }

    public Response build() {
      return new Response(statusCode, fixedDelay);
    }
  }
}
