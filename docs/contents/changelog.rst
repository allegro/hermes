Changelog
=========

Hermes 0.8.0
------------

Release date: **07.08.2015**

Message tracking
^^^^^^^^^^^^^^^^

Refactored message tracking mechanisms, and created new module: *hermes-tracker* that contains all interfaces and base
classes needed to implement own message tracker repository. Along with Hermes we publish two message tracker repository
implementations:

* Mongo (in *tracker-mongo*)
* ElasticSearch (in *tracker-elasticsearch*)

Performance
^^^^^^^^^^^

On publishing side, we mitigated issue with latency peaks observed for 99th and 99.9th percentile of incoming requests. This
was due to operating in wrong thread pool, which could lead to blocks under high contention.
On consumer side, we managed to decrease heap size. We also made corrections to our rate limiting algorithm. It is
now more fault tolerant, so that couple malformed events do not make sending speed decrease dramatically.

HTTP/2
^^^^^^

We added full support for HTTP/2 on both publisher and consumer side. Hermes can operate in mixed HTTP/1.1 and HTTP/2 mode.
This brings us closer to data streaming with Avro (which will be fully supported in next major release).

Hermes 0.7.0
------------

Initial OpenSource release.