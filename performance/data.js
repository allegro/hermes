window.BENCHMARK_DATA = {
  "lastUpdate": 1772458741961,
  "repoUrl": "https://github.com/allegro/hermes",
  "entries": {
    "Hermes Benchmark": [
      {
        "commit": {
          "author": {
            "email": "maciej.sabat@allegro.com",
            "name": "Maciej Sabat",
            "username": "maciejsabat-allegro"
          },
          "committer": {
            "email": "maciej.sabat@allegro.com",
            "name": "Maciej Sabat",
            "username": "maciejsabat-allegro"
          },
          "distinct": true,
          "id": "12bfde353c0a8f951f232c85024aa743187ce384",
          "message": "Add performance tests charts",
          "timestamp": "2026-03-02T14:30:31+01:00",
          "tree_id": "1b78416356a4041c8cded8041c825164f7b6669c",
          "url": "https://github.com/allegro/hermes/commit/12bfde353c0a8f951f232c85024aa743187ce384"
        },
        "date": 1772458719433,
        "tool": "customSmallerIsBetter",
        "benches": [
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 7.124999389719839,
            "range": "0.45888606228629175",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 210503799.84964862,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 8.030768580111113,
            "range": "0.06416389524717095",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 153630856.72337162,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt)",
            "value": 7135.855805465049,
            "range": "184.84861640944754",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 68172.22643077845,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt)",
            "value": 325252.60230375314,
            "range": "20693.6880012775",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt) - gc.alloc.rate.norm",
            "value": 9104.109732839745,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt)",
            "value": 324037.5033070583,
            "range": "18040.36420397949",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt) - gc.alloc.rate.norm",
            "value": 9160.09954406219,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample)",
            "value": 0.29928389903882135,
            "range": "0.004833770322275316",
            "unit": "ms/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample) - gc.alloc.rate.norm",
            "value": 68141.45694793329,
            "unit": "B/op"
          }
        ]
      }
    ]
  }
}