package pl.allegro.tech.hermes.client;

import java.util.Optional;
import java.util.function.Function;

public class HermesResponseBuilder {

  private int statusCode = -1;
  private String body = "";
  private String protocol = "http/1.1";
  private Throwable failureCause;
  private Function<String, String> headerSupplier = (header) -> null;
  private HermesMessage hermesMessage;

  public static HermesResponseBuilder hermesResponse(HermesMessage hermesMessage) {
    return new HermesResponseBuilder().withHermesMessage(hermesMessage);
  }

  public static HermesResponse hermesFailureResponse(
      Throwable exception, HermesMessage hermesMessage) {
    return hermesResponse(hermesMessage).withFailureCause(exception).build();
  }

  public HermesResponseBuilder withHttpStatus(int statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  public HermesResponseBuilder withBody(String body) {
    this.body = body;
    return this;
  }

  private HermesResponseBuilder withFailureCause(Throwable exception) {
    this.failureCause = exception;
    return this;
  }

  private HermesResponseBuilder withHermesMessage(HermesMessage hermesMessage) {
    this.hermesMessage = hermesMessage;
    return this;
  }

  public HermesResponseBuilder withHeaderSupplier(Function<String, String> headerSupplier) {
    this.headerSupplier = headerSupplier;
    return this;
  }

  public HermesResponseBuilder withProtocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  public HermesResponse build() {
    return new HermesResponse() {

      @Override
      public int getHttpStatus() {
        return statusCode;
      }

      @Override
      public HermesMessage getHermesMessage() {
        return hermesMessage;
      }

      @Override
      public Optional<Throwable> getFailureCause() {
        return Optional.ofNullable(failureCause);
      }

      @Override
      public Optional<HermesMessage> getFailedMessage() {
        return Optional.ofNullable(hermesMessage);
      }

      @Override
      public String getBody() {
        return body;
      }

      @Override
      public String getHeader(String header) {
        return headerSupplier.apply(header);
      }

      @Override
      public String getProtocol() {
        return protocol;
      }
    };
  }
}
