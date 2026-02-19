package pl.allegro.tech.hermes.benchmark.consumer;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@State(Scope.Benchmark)
@Threads(1)
public class HermesConsumerBenchmark {

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public void benchmarkConsumingThroughput(ConsumerEnvironment env) {
    env.startConsumer();
    env.waitUntilAllMessagesAreConsumed();
  }

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(".*" + HermesConsumerBenchmark.class.getSimpleName() + ".*")
            .warmupIterations(4)
            .measurementIterations(4)
            .measurementTime(TimeValue.seconds(5))
            .warmupTime(TimeValue.seconds(2))
            .forks(1)
            .threads(1)
            .syncIterations(false)
            .build();

    new Runner(opt).run();
  }
}
