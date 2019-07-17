package pl.allegro.tech.hermes.test.helper.util;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

public final class Ports {

    private Ports() {
    }

    public static int nextAvailable() {
        try {
            try (ServerSocket socket = new ServerSocket(0)) {
                socket.setReuseAddress(true);
                return socket.getLocalPort();
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
