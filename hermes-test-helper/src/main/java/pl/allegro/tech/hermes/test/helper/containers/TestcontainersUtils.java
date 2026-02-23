package pl.allegro.tech.hermes.test.helper.containers;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

public class TestcontainersUtils {

  private static final Logger logger = LoggerFactory.getLogger(TestcontainersUtils.class);

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

  /**
   * Executes a command inside a container with retry logic to handle transient Docker daemon
   * failures such as {@code NoHttpResponseException}.
   */
  public static ExecResult execInContainerWithRetry(
      ContainerState container, int maxAttempts, Duration delayBetweenAttempts, String... command)
      throws InterruptedException {
    RuntimeException lastException = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        return container.execInContainer(command);
      } catch (IOException e) {
        lastException = new RuntimeException(e);
      } catch (RuntimeException e) {
        lastException = e;
      }
      if (attempt < maxAttempts) {
        logger.warn(
            "execInContainer failed on attempt {}/{}: {}. Retrying in {}ms...",
            attempt,
            maxAttempts,
            lastException.getMessage(),
            delayBetweenAttempts.toMillis());
        Thread.sleep(delayBetweenAttempts.toMillis());
      }
    }
    throw lastException;
  }

  /**
   * Starts a container with retry logic to handle transient Docker daemon failures such as {@code
   * NoHttpResponseException} caused by a temporarily overloaded Docker daemon.
   *
   * <p>Testcontainers has its own internal fast-retry loop, but it exhausts quickly without
   * meaningful delays between attempts. This outer retry adds a pause between attempts, giving the
   * Docker daemon time to recover before trying again.
   */
  public static void startWithRetry(
      Runnable startAction, int maxAttempts, Duration delayBetweenAttempts) {
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        startAction.run();
        return;
      } catch (RuntimeException e) {
        if (attempt == maxAttempts) {
          throw e;
        }
        logger.warn(
            "Container start failed on attempt {}/{}: {}. Retrying in {}ms...",
            attempt,
            maxAttempts,
            e.getMessage(),
            delayBetweenAttempts.toMillis());
        try {
          Thread.sleep(delayBetweenAttempts.toMillis());
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Interrupted while waiting to retry container start", ie);
        }
      }
    }
  }
}
