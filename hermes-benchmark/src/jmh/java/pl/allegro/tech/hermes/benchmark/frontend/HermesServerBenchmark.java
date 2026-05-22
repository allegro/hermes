package pl.allegro.tech.hermes.benchmark.frontend;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;
import pl.allegro.tech.hermes.benchmark.config.JmhRunner;

@Fork(1)
@Threads(2)
@Warmup(iterations = 6, time = 4, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 6, time = 6, timeUnit = TimeUnit.SECONDS)
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
    JmhRunner.runBenchmark(HermesServerBenchmark.class);
  }
}
