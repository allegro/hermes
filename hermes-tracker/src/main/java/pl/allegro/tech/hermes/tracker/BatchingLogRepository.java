package pl.allegro.tech.hermes.tracker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BatchingLogRepository<T> {

  protected final String clusterName;
  protected final String hostname;
  protected BlockingQueue<T> queue;

  public BatchingLogRepository(int queueSize, String clusterName, String hostname) {
    this.queue = new LinkedBlockingQueue<>(queueSize);
    this.clusterName = clusterName;
    this.hostname = hostname;
  }
}
