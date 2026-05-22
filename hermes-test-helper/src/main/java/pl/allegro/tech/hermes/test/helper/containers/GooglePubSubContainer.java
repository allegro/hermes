package pl.allegro.tech.hermes.test.helper.containers;

import java.time.Duration;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

public class GooglePubSubContainer extends PubSubEmulatorContainer {

  private static final String DOCKER_IMAGE =
      "gcr.io/google.com/cloudsdktool/cloud-sdk:367.0.0-emulators";

  public GooglePubSubContainer() {
    super(DockerImageName.parse(DOCKER_IMAGE));
  }

  @Override
  public void start() {
    TestcontainersUtils.startWithRetry(super::start, 3, Duration.ofSeconds(2));
  }
}
