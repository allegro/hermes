package pl.allegro.tech.hermes.benchmark.consumer;

import static com.google.common.collect.ImmutableMap.of;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

@State(Scope.Thread)
@Threads(1)
public class HermesConsumerFilteringBenchmark {

  private static final int MESSAGES_COUNT = 100_000;
  private ConsumerEnvironment consumerEnvironment;

  @Setup
  public void setupBeforeAll(Blackhole blackhole) {
    consumerEnvironment = new ConsumerEnvironment(blackhole);
  }

  @Setup(Level.Iteration)
  public void setup() {
    MessageFilterSpecification filter =
        new MessageFilterSpecification(
            of("type", "avropath", "path", ".name", "matcher", "Robert"));

    consumerEnvironment.createFilteringConsumer(
        HermesConsumerFilteringBenchmark::createMessage, filter, MESSAGES_COUNT);
  }

  private static AvroUser createMessage(int i) {
    return new AvroUser(i < MESSAGES_COUNT / 2 ? "Mark" : "Robert", i, "blue");
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public void benchmarkConsumingThroughput() {
    consumerEnvironment.startConsumer();
    consumerEnvironment.waitUntilAllMessagesAreConsumed(MESSAGES_COUNT / 2);
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
    Options opt =
        new OptionsBuilder()
            .include(".*" + HermesConsumerFilteringBenchmark.class.getSimpleName() + ".*")
            .warmupIterations(6)
            .measurementIterations(6)
            .measurementTime(TimeValue.seconds(5))
            .warmupTime(TimeValue.seconds(2))
            .forks(1)
            .threads(1)
            .syncIterations(false)
            .addProfiler(GCProfiler.class)
            .build();

    new Runner(opt).run();
  }
}
