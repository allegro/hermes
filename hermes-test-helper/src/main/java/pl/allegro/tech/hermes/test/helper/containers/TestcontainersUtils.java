package pl.allegro.tech.hermes.test.helper.containers;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

public class TestcontainersUtils {

  public static void copyScriptToContainer(
      String content, ContainerState containerState, String target) {
    containerState.copyFileToContainer(
        Transferable.of(content.getBytes(StandardCharsets.UTF_8), 0777), target);
  }

  public static String readFileFromClasspath(String fileName) throws IOException {
    InputStream inputStream =
        TestcontainersUtils.class.getClassLoader().getResourceAsStream(fileName);
    if (inputStream == null) {
      throw new FileNotFoundException("File '" + fileName + "' does not exist");
    }
    return IOUtils.toString(inputStream, UTF_8);
  }
}
