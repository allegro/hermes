package pl.allegro.tech.hermes.test.helper.zookeeper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.rules.ExternalResource;

public class ZookeeperResource extends ExternalResource {

  private static Starter zookeeperStarter;

  private final int curatorPort;

  private final boolean initializeOnce;

  private final Consumer<Starter> initializer;

  public ZookeeperResource(int curatorPort, boolean initializeOnce, Consumer<Starter> initializer) {
    this.curatorPort = curatorPort;
    this.initializeOnce = initializeOnce;
    this.initializer = initializer;
  }

  public CuratorFramework curator() {
    return zookeeperStarter.curator();
  }

  @Override
  protected void before() throws Throwable {
    if (initializeOnce) {
      if (zookeeperStarter == null) {
        zookeeperStarter = new Starter(curatorPort, initializer);
        zookeeperStarter.start();
      }
    } else {
      zookeeperStarter = new Starter(curatorPort, initializer);
      zookeeperStarter.start();
    }
  }

  @Override
  protected void after() {
    if (!initializeOnce) {
      zookeeperStarter.stop();
    } else {
      zookeeperStarter.registerShutdownHook();
    }
  }

  public static final class Starter {

    private final int curatorPort;

    private final Consumer<Starter> initializer;

    private TestingServer server;

    private CuratorFramework curator;

    Starter(int curatorPort, Consumer<Starter> initializer) {
      this.curatorPort = curatorPort;
      this.initializer = initializer;
    }

    public CuratorFramework curator() {
      return curator;
    }

    void start() {
      try {
        server = new TestingServer(curatorPort, true);
        this.curator =
            CuratorFrameworkFactory.newClient(server.getConnectString(), new RetryOneTime(1000));
        this.curator.start();

        this.curator.blockUntilConnected(10, TimeUnit.SECONDS);
        initializer.accept(this);
      } catch (Exception ex) {
        throw new IllegalStateException("Failed to start Zookeeper", ex);
      }
    }

    void stop() {
      try {
        this.curator.close();
        this.server.close();
      } catch (IOException ex) {
        throw new IllegalStateException("Failed to stop zookeeper", ex);
      }
    }

    void registerShutdownHook() {
      Runtime.getRuntime().addShutdownHook(new Thread(Starter.this::stop));
    }
  }
}
