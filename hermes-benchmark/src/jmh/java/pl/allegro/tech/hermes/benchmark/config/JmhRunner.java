package pl.allegro.tech.hermes.benchmark.config;

import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class JmhRunner {
  public static void runBenchmark(Class<?> benchmarkClass) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(".*" + benchmarkClass.getSimpleName() + ".*")
            .syncIterations(false)
            .addProfiler(GCProfiler.class)
            .build();
    new Runner(opt).run();
  }
}
