package pl.allegro.tech.hermes.benchmark;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import pl.allegro.tech.hermes.benchmark.environment.HermesServerEnvironment;

@State(Scope.Benchmark)
public class HermesServerBenchmark {

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public int benchmarkPublishingThroughput(HermesServerEnvironment hermesServerEnvironment) {
    return hermesServerEnvironment.publisher().publish();
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public int benchmarkPublishingLatency(HermesServerEnvironment hermesServerEnvironment) {
    return hermesServerEnvironment.publisher().publish();
  }

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(".*" + HermesServerBenchmark.class.getSimpleName() + ".*")
            .warmupIterations(4)
            .measurementIterations(4)
            // .addProfiler(JmhFlightRecorderProfiler.class)
            // .jvmArgs("-XX:+UnlockCommercialFeatures")
            .measurementTime(TimeValue.seconds(60))
            .warmupTime(TimeValue.seconds(40))
            .forks(1)
            .threads(2)
            .syncIterations(false)
            .build();

    new Runner(opt).run();
  }
}
