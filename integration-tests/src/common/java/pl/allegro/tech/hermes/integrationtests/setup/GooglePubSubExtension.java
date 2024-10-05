package pl.allegro.tech.hermes.integrationtests.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import pl.allegro.tech.hermes.integrationtests.subscriber.TestGooglePubSubSubscriber;
import pl.allegro.tech.hermes.test.helper.containers.GooglePubSubContainer;

public class GooglePubSubExtension implements BeforeAllCallback, AfterAllCallback {

  private final GooglePubSubContainer googlePubSubContainer = new GooglePubSubContainer();
  private final List<TestGooglePubSubSubscriber> subscribers = new ArrayList<>();

  @Override
  public void beforeAll(ExtensionContext context) {
    googlePubSubContainer.start();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    for (TestGooglePubSubSubscriber subscriber : subscribers) {
      subscriber.stop();
    }
    googlePubSubContainer.stop();
  }

  public TestGooglePubSubSubscriber subscriber() throws IOException {
    TestGooglePubSubSubscriber subscriber = new TestGooglePubSubSubscriber(getEmulatorEndpoint());
    subscribers.add(subscriber);
    return subscriber;
  }

  public String getEmulatorEndpoint() {
    return googlePubSubContainer.getEmulatorEndpoint();
  }
}
