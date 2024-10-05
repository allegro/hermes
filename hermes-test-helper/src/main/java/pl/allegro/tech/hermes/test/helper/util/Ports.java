package pl.allegro.tech.hermes.test.helper.util;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ports {

  private static final Logger logger = LoggerFactory.getLogger(Ports.class);

  private Ports() {}

  public static int nextAvailable() {
    try {
      ServerSocket socket = new ServerSocket(0);
      socket.setReuseAddress(true);
      int port = socket.getLocalPort();
      socket.getLocalSocketAddress();
      socket.close();

      // second check whether the port is available as on some dynamic environments it can be still
      // in use
      try (Socket ignore = new Socket("127.0.0.1", port)) {
        logger.warn(
            "Connected to randomly selected port {} meaning it is still in use. Drawing next port.",
            port);
        return nextAvailable();
      } catch (ConnectException ex) {
        // expected exception as on provided port no one should listen
        return port;
      }
    } catch (IOException exception) {
      throw new NoAvailablePortException(exception);
    }
  }

  public static int nextAvailable(int min, int max) {
    for (int port = min; port <= max; ++port) {
      if (isPortAvailable(port)) {
        return port;
      }
    }
    throw new NoAvailablePortException("Problem finding port in a scope: " + min + " " + max);
  }

  public static boolean isPortAvailable(int port) {
    try {
      try (ServerSocket socket = new ServerSocket(port)) {
        socket.setReuseAddress(true);

        try (DatagramSocket datagramSocket = new DatagramSocket(port)) {
          datagramSocket.setReuseAddress(true);
          return true;
        }
      }
    } catch (IOException e) {
      return false;
    }
  }

  public static class NoAvailablePortException extends RuntimeException {
    public NoAvailablePortException(Throwable cause) {
      super(cause);
    }

    public NoAvailablePortException(String message) {
      super(message);
    }
  }
}
