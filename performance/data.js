window.BENCHMARK_DATA = {
  "lastUpdate": 1772534808560,
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
          "id": "6a3f5fc2dbb910e76f8fc80b522557d4c7c90429",
          "message": "Fix typo",
          "timestamp": "2026-03-03T10:43:04+01:00",
          "tree_id": "7bc203dec46824a355d0dfbdce6bdad1a2115d47",
          "url": "https://github.com/allegro/hermes/commit/6a3f5fc2dbb910e76f8fc80b522557d4c7c90429"
        },
        "date": 1772531444397,
        "tool": "customSmallerIsBetter",
        "benches": [
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 7.428443268871028,
            "range": "0.09074104210017749",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 206618193.77018046,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 8.223391315458388,
            "range": "0.30924901740399385",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 153246397.96321842,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt)",
            "value": 7466.156864186446,
            "range": "321.6840466161395",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 68185.48718453887,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt)",
            "value": 301050.6838928261,
            "range": "30947.588235549018",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt) - gc.alloc.rate.norm",
            "value": 9104.101125368192,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt)",
            "value": 310281.64696951705,
            "range": "17249.158476882425",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt) - gc.alloc.rate.norm",
            "value": 9160.083938688009,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample)",
            "value": 0.26903855494373907,
            "range": "0.004197544722442068",
            "unit": "ms/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample) - gc.alloc.rate.norm",
            "value": 68044.22662104869,
            "unit": "B/op"
          }
        ]
      },
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
          "id": "5e6e9d4a1cfbc60e102d2e55c1d161a3dfc4da54",
          "message": "Add performance tests charts",
          "timestamp": "2026-03-03T11:38:19+01:00",
          "tree_id": "21d66f8c73c1ce827362abaa99fda293daf051ec",
          "url": "https://github.com/allegro/hermes/commit/5e6e9d4a1cfbc60e102d2e55c1d161a3dfc4da54"
        },
        "date": 1772534789446,
        "tool": "customSmallerIsBetter",
        "benches": [
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 7.30902575548106,
            "range": "0.34171949757448095",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 205533479.4843305,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 8.074922544089363,
            "range": "0.29215261711734924",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 152808341.49118775,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt)",
            "value": 7036.415433375386,
            "range": "248.27681815335762",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 68171.16193346953,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt)",
            "value": 326455.88587644853,
            "range": "18675.52502954533",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt) - gc.alloc.rate.norm",
            "value": 9104.110292564199,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt)",
            "value": 327481.3793173984,
            "range": "11534.517525095478",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt) - gc.alloc.rate.norm",
            "value": 9160.10839280628,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample)",
            "value": 0.28452846930159514,
            "range": "0.00456218504603578",
            "unit": "ms/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample) - gc.alloc.rate.norm",
            "value": 68157.14713781976,
            "unit": "B/op"
          }
        ]
      }
    ]
  }
}