package pl.allegro.tech.hermes.benchmark;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import pl.allegro.tech.hermes.benchmark.environment.SubscriberEnvironment;

@State(Scope.Benchmark)
@Threads(1)
public class HermesConsumerBenchmark {

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.MINUTES)
  public void benchmarkConsumingThroughput(SubscriberEnvironment env) {
    env.waitUntilAllMessagesAreConsumed();
  }

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(".*" + HermesConsumerBenchmark.class.getSimpleName() + ".*")
            .warmupIterations(4)
            .measurementIterations(4)
            .measurementTime(TimeValue.seconds(60))
            .warmupTime(TimeValue.seconds(40))
            .forks(1)
            .threads(1)
            .syncIterations(false)
            .build();

    new Runner(opt).run();
  }
}
