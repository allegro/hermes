package pl.allegro.tech.hermes.benchmark.consumer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;
import pl.allegro.tech.hermes.benchmark.config.JmhRunner;

@State(Scope.Thread)
@Threads(1)
@Fork(1)
@Warmup(iterations = 6, time = 7, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 6, time = 5, timeUnit = TimeUnit.SECONDS)
public class HermesConsumerBenchmark {

  private static final int MESSAGES_COUNT = 100_000;
  private static final Duration TIMEOUT = Duration.ofSeconds(4);
  private ConsumerEnvironment consumerEnvironment;

  @Setup
  public void setupBeforeAll(Blackhole blackhole) {
    consumerEnvironment = new ConsumerEnvironment(blackhole);
  }

  @Setup(Level.Iteration)
  public void setup() {
    consumerEnvironment.createConsumer(MESSAGES_COUNT);
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public void benchmarkConsumingThroughput() {
    consumerEnvironment.startConsumer();
    consumerEnvironment.waitUntilAllMessagesAreConsumed(MESSAGES_COUNT, TIMEOUT);
  }

  @TearDown(Level.Iteration)
  public void stopConsumer() {
    consumerEnvironment.stopConsumer();
  }

  @TearDown
  public void shutDownAfterAll() {
    consumerEnvironment.shutDownAfterAll();
  }

  public static void main(String[] args) throws RunnerException {
    JmhRunner.runBenchmark(HermesConsumerBenchmark.class);
  }
}
