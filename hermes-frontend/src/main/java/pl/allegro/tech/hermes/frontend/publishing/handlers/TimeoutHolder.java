package pl.allegro.tech.hermes.frontend.publishing.handlers;

import java.util.Optional;
import java.util.function.Consumer;
import org.xnio.XnioExecutor;

class TimeoutHolder {

  private final XnioExecutor.Key timeoutKey;
  private final int timeout;
  private volatile Optional<Consumer<Void>> timeoutConsumer = Optional.empty();

  TimeoutHolder(int timeout, XnioExecutor.Key timeoutKey) {
    this.timeoutKey = timeoutKey;
    this.timeout = timeout;
  }

  public boolean remove() {
    return timeoutKey.remove();
  }

  public void onTimeout(Consumer<Void> timeoutConsumer) {
    this.timeoutConsumer = Optional.of(timeoutConsumer);
  }

  public void timeout() {
    timeoutConsumer.ifPresent(c -> c.accept(null));
  }

  public int getTimeout() {
    return timeout;
  }
}
