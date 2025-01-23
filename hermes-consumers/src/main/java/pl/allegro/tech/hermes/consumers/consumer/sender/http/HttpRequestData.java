package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.api.EndpointAddress;

public class HttpRequestData {

  private EndpointAddress rawAddress;

  public HttpRequestData(EndpointAddress rawAddress) {
    this.rawAddress = rawAddress;
  }

  public EndpointAddress getRawAddress() {
    return rawAddress;
  }

  public static class HttpRequestDataBuilder {
    private EndpointAddress rawAddress;

    public HttpRequestDataBuilder withRawAddress(EndpointAddress rawAddress) {
      this.rawAddress = rawAddress;
      return this;
    }

    public HttpRequestData build() {
      return new HttpRequestData(rawAddress);
    }
  }
}
