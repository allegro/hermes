package pl.allegro.tech.hermes.test.helper.containers;

import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

public class GooglePubSubContainer extends PubSubEmulatorContainer {

  private static final String DOCKER_IMAGE =
      "gcr.io/google.com/cloudsdktool/cloud-sdk:367.0.0-emulators";

  public GooglePubSubContainer() {
    super(DockerImageName.parse(DOCKER_IMAGE));
  }
}
