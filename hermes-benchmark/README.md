Hermes-benchmark
======

Hermes benchmarks written in [JMH](http://openjdk.java.net/projects/code-tools/jmh/). 

Running all benchmarks
---
```
./gradlew jmh
```

Running a specific benchmark with custom params
--
```
./gradlew jmh -Pjmh.iterations=6  -Pjmh.timeOnIteration=5s -Pjmh.warmupIterations=6 -Pjmh.timeOnWarmupIteration=2s -Pjmh.includes="HermesConsumerBenchmark"
```

Interpreting results
--
When you run a benchmark, you'll see a summary table at the end with the following columns:

* Benchmark - the name of your benchmark method
* Mode - the benchmark mode (e.g., thrpt, avgt, sample)
* Cnt - Number of iterations
* Score - The mean value of your metric
* Error - The statistical uncertainty. Please keep in mind that benchmarks with an error margin higher than 10% is considered unreliable.
* Units - Units of measurement (e.g., ops/s or ns/op)