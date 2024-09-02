package pl.allegro.tech.hermes.integrationtests.setup;

import org.hornetq.jms.server.embedded.EmbeddedJMS;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

public class JmsStarter implements Starter<EmbeddedJMS> {

  private EmbeddedJMS jmsServer;

  @Override
  public void start() throws Exception {
    jmsServer = new EmbeddedJMS();
    jmsServer.start();
  }

  @Override
  public void stop() throws Exception {
    jmsServer.stop();
  }

  @Override
  public EmbeddedJMS instance() {
    return jmsServer;
  }
}
