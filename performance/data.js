window.BENCHMARK_DATA = {
  "lastUpdate": 1773925964609,
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
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "cafe1f22691bc275090dfbb3297c0b1b337b2116",
          "message": "Fix GH pages (#2029)\n\n* Fix GH pages\n\n* gh pages fix\n\n* gh pages fix",
          "timestamp": "2026-03-06T08:11:40+01:00",
          "tree_id": "b4ea11b48ca29e5a7246666fd5def1523ed358f1",
          "url": "https://github.com/allegro/hermes/commit/cafe1f22691bc275090dfbb3297c0b1b337b2116"
        },
        "date": 1772781559089,
        "tool": "customSmallerIsBetter",
        "benches": [
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 7.238354092838491,
            "range": "0.08791441881520665",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 204426088.99525166,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 7.875180858565013,
            "range": "0.3875145426990041",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 155593350.62375477,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt)",
            "value": 7092.142233016409,
            "range": "206.3708912304574",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 67874.4033717908,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt)",
            "value": 323745.1986697307,
            "range": "17758.88412007924",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt) - gc.alloc.rate.norm",
            "value": 9104.10897302917,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt)",
            "value": 334105.6391537325,
            "range": "24656.266918806763",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt) - gc.alloc.rate.norm",
            "value": 9160.1067643107,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample)",
            "value": 0.29517912170936567,
            "range": "0.004101162008235535",
            "unit": "ms/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample) - gc.alloc.rate.norm",
            "value": 68333.20999765977,
            "unit": "B/op"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "michal.dabrowski@allegro.com",
            "name": "Michał Dąbrowski",
            "username": "michaldabrowski"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "407f71bbbf30f45a0525a15e59c304104c6260ba",
          "message": "Stabilize integration-tests (#2028)",
          "timestamp": "2026-03-06T11:19:51+01:00",
          "tree_id": "22ae599e5e923ec267f75c1ed6ffe490ae8b606a",
          "url": "https://github.com/allegro/hermes/commit/407f71bbbf30f45a0525a15e59c304104c6260ba"
        },
        "date": 1772792848719,
        "tool": "customSmallerIsBetter",
        "benches": [
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 7.235476404489572,
            "range": "0.08107392417507435",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 205992680.0037987,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 7.989959282147894,
            "range": "0.286163162868545",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 154154949.36398467,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt)",
            "value": 7001.948097996337,
            "range": "190.6467519327822",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 68186.48719994166,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt)",
            "value": 324490.6868936546,
            "range": "15493.36570201202",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt) - gc.alloc.rate.norm",
            "value": 9104.109777322019,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt)",
            "value": 313681.2007043488,
            "range": "19826.387935018425",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt) - gc.alloc.rate.norm",
            "value": 9160.096762343828,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample)",
            "value": 0.2875539314680115,
            "range": "0.005005826528567361",
            "unit": "ms/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample) - gc.alloc.rate.norm",
            "value": 68148.56371612473,
            "unit": "B/op"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "144275833+MikitaBychkou@users.noreply.github.com",
            "name": "Mikita",
            "username": "MikitaBychkou"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "ad89582251c23c442bb888302c983684fa01a781",
          "message": "Update default topic retention from 7 to 2 days (#2030)\n\n* Update retention on topics\n\n* Fix retention time tests",
          "timestamp": "2026-03-13T14:12:58+01:00",
          "tree_id": "80e62a546c031c7c6bca428a33b4514b6affb8fa",
          "url": "https://github.com/allegro/hermes/commit/ad89582251c23c442bb888302c983684fa01a781"
        },
        "date": 1773408038669,
        "tool": "customSmallerIsBetter",
        "benches": [
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 7.221290542426636,
            "range": "0.09442141732546326",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 206685200.28869894,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 7.856737775984282,
            "range": "0.4395612034906648",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 156956841.9816092,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt)",
            "value": 6638.675229160144,
            "range": "175.9278464805516",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 68154.9498271794,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt)",
            "value": 327812.84633240005,
            "range": "18612.02449643485",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt) - gc.alloc.rate.norm",
            "value": 9104.110731110102,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt)",
            "value": 324833.8270972275,
            "range": "18159.121297486796",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt) - gc.alloc.rate.norm",
            "value": 9160.105335827557,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample)",
            "value": 0.3075062089559896,
            "range": "0.00612011288403657",
            "unit": "ms/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample) - gc.alloc.rate.norm",
            "value": 68197.28071859805,
            "unit": "B/op"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "87751153+adamallegro@users.noreply.github.com",
            "name": "Adam Szorcz",
            "username": "adamallegro"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "fce73288ee3b409449fe79af05dc8d49b793c50a",
          "message": "Recreate BigQuery SW API Writer every time when the schema of table changes (#2031)\n\n* TANGO-2997 : Basic functionality which should work\n\n* TANGO-2997 : Logging\n\n* TANGO-2997 : Fixed types\n\n* TANGO-2997 : Removed unnecessary buggy line\n\n* TANGO-2997 : Restarting the client - taking the counter into consideration\n\n* TANGO-2997 : Refactor and unique way of handling errors in all outs\n\n* TANGO-2997 : task related logging\n\n* TANGO-2997 : Removed task-related logging and method rename\n\n* TANGO-2997 : Time based decision if release all clients\n\n* TANGO-2997 : Added custom exception\n\n* TANGO-2997 : Added passing the exception\n\n* TANGO-2997 : Reformatted\n\n* TANGO-2997 : Reformatted\n\n* TANGO-2997 : Changed condition\n\n* TANGO-2997 : Reset instead of Remove\n\n* TANGO-2997 : Fixes after review\n\n* TANGO-2997 : Minor commontes and format changes\n\n* Potential fix for pull request finding\n\nCo-authored-by: Copilot Autofix powered by AI <175728472+Copilot@users.noreply.github.com>\n\n* Apply suggestions from code review\n\nCo-authored-by: Copilot Autofix powered by AI <175728472+Copilot@users.noreply.github.com>\n\n* Potential fix for pull request finding\n\nCo-authored-by: Copilot Autofix powered by AI <175728472+Copilot@users.noreply.github.com>\n\n* Potential fix for pull request finding\n\nCo-authored-by: Copilot Autofix powered by AI <175728472+Copilot@users.noreply.github.com>\n\n* Potential fix for pull request finding\n\nCo-authored-by: Copilot Autofix powered by AI <175728472+Copilot@users.noreply.github.com>\n\n* Potential fix for pull request finding\n\nCo-authored-by: Copilot Autofix powered by AI <175728472+Copilot@users.noreply.github.com>\n\n* TANGO-2997 : Removed unused class and its test\n\n* TANGO-2997 : Brought back Exception catching\n\n---------\n\nCo-authored-by: Copilot Autofix powered by AI <175728472+Copilot@users.noreply.github.com>",
          "timestamp": "2026-03-19T14:04:10+01:00",
          "tree_id": "f05ddedcb995e062b6c62edafc73c23d3b581917",
          "url": "https://github.com/allegro/hermes/commit/fce73288ee3b409449fe79af05dc8d49b793c50a"
        },
        "date": 1773925910428,
        "tool": "customSmallerIsBetter",
        "benches": [
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 7.5057193077968085,
            "range": "0.70379077876038",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 197337577.99782935,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt)",
            "value": 8.058996657214564,
            "range": "0.019468239304703587",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.consumer.HermesConsumerFilteringBenchmark.benchmarkConsumingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 151598895.6888889,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt)",
            "value": 6869.890346850465,
            "range": "213.0541221876069",
            "unit": "ops/s"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingThroughput (thrpt) - gc.alloc.rate.norm",
            "value": 68227.50407521785,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt)",
            "value": 317032.29718957475,
            "range": "24038.30795625677",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.baselineSave (avgt) - gc.alloc.rate.norm",
            "value": 9104.10598783432,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt)",
            "value": 325061.55741757085,
            "range": "20582.557790469335",
            "unit": "ns/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.MessageRepositoryBenchmark.hermesImplSave (avgt) - gc.alloc.rate.norm",
            "value": 9160.105198370848,
            "unit": "B/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample)",
            "value": 0.2928782918441651,
            "range": "0.004647340008881686",
            "unit": "ms/op"
          },
          {
            "name": "pl.allegro.tech.hermes.benchmark.frontend.HermesServerBenchmark.benchmarkPublishingLatency (sample) - gc.alloc.rate.norm",
            "value": 68141.80194818527,
            "unit": "B/op"
          }
        ]
      }
    ]
  }
}