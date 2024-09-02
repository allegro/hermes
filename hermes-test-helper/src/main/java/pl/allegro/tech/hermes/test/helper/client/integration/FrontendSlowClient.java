package pl.allegro.tech.hermes.test.helper.client.integration;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrontendSlowClient {

  public static final String MSG_BODY = "{\"field\": \"value\"}";

  public String msgHeadWithContentLenght(String topicName) {
    return "POST /topics/"
        + topicName
        + " HTTP/1.1\n"
        + "Host: "
        + host
        + ":"
        + port
        + "\n"
        + "Content-Type: application/json\n"
        + "Content-Length: "
        + MSG_BODY.length()
        + "\r\n\r\n";
  }

  private String msgHeadWithChunkedEncoding(String topicName) {
    return "POST /topics/"
        + topicName
        + " HTTP/1.1\n"
        + "Host: "
        + host
        + ":"
        + port
        + "\n"
        + "Content-Type: application/json\n"
        + "Transfer-Encoding: chunked \r\n\r\n";
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(FrontendSlowClient.class);

  private final String host;
  private final int port;

  public FrontendSlowClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public String slowEvent(
      int clientTimeout,
      int pauseTimeBetweenChunks,
      int delayBeforeSendingFirstData,
      String topicName,
      boolean chunkedEncoding)
      throws IOException, InterruptedException {

    Socket clientSocket = new Socket(host, port);

    Thread.sleep(delayBeforeSendingFirstData);

    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    BufferedReader inFromServer =
        new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

    clientSocket.setSoTimeout(clientTimeout);

    if (chunkedEncoding) {
      outToServer.writeBytes(msgHeadWithChunkedEncoding(topicName));
      outToServer.flush();
    } else {
      outToServer.writeBytes(msgHeadWithContentLenght(topicName));
      outToServer.flush();
    }
    sendBodyInChunks(outToServer, MSG_BODY, pauseTimeBetweenChunks, chunkedEncoding);

    String response;
    try {
      response = readResponse(inFromServer);
      LOGGER.info("Response: {}", response);
    } catch (SocketTimeoutException e) {
      LOGGER.warn("client timeout");
      clientSocket.close();
      throw e;
    }

    clientSocket.close();

    return response;
  }

  private void sendBodyInChunks(
      DataOutputStream outToServer, String msgBody, int pauseTime, boolean encoded) {
    executor.execute(
        () -> {
          try {
            for (int index = 0; index < msgBody.length(); index++) {
              outToServer.writeBytes(prepareChunk(msgBody, index, encoded));
              outToServer.flush();
              LOGGER.info("Sent chunk");
              if (pauseTime > 0) {
                Thread.sleep(pauseTime);
              }
            }
            if (encoded) {
              outToServer.writeBytes("0\r\n\r\n");
              outToServer.flush();
              LOGGER.info("Finished chunked encoding");
            }
          } catch (SocketException e) {
            LOGGER.warn("Socket closed");
          } catch (InterruptedException | IOException e) {
            LOGGER.error("Something went wrong while sending data", e);
          }
        });
  }

  private String prepareChunk(String msg, int index, boolean encoded) {
    return encoded
        ? String.format("1\n%c\r\n", msg.charAt(index))
        : String.valueOf(msg.charAt(index));
  }

  private String readResponse(BufferedReader bufferedReader) throws IOException {
    String line;
    StringBuilder response = new StringBuilder();

    while (!(line = bufferedReader.readLine()).isEmpty()) {
      response.append(line);
    }

    return response.toString();
  }
}
