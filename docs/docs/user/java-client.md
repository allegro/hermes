# Java Client

A thin library designed to publish messages to Hermes.

## Features

* http client-agnostic API
* synchronous/asynchronous publishing
* configurable retries
* metrics

## Overview

Core functionality is provided by `HermesClient` class, which in turn uses `HermesSender` to do the heavy lifting.
At the moment there are three implementations of `HermesSender`:

* **RestTemplateHermesSender** - recommended for services built on [Spring framework](http://projects.spring.io/spring-framework);
  uses [AsyncRestTemplate](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/AsyncRestTemplate.html)
  for asynchronous transmission
* **JerseyHermesSender** - recommended for services using  [Jersey](<https://jersey.java.net/>)
* **OkHttpHermesSender** - supports both HTTP/1.1 and HTTP/2 protocols, uses [OkHttp client](http://square.github.io/okhttp/)


## Getting started

To start using `HermesClient`, add it as an dependency:

```groovy
compile group: 'pl.allegro.tech.hermes', name: 'hermes-client', version: versions.hermes
```

Client should be always built using `HermesClientBuilder`, which allows on setting:

```java
HermesClientBuilder.hermesClient(...)
    .withURI(...) // Hermes URI
    .withRetries(...) // how many times retry in case of errors, default: 3
    .withDefaultContentType(...) // what Content-Type to use when none set, default: application/json
    .withMetrics(metricsRegistry) // see Metrics section below
    .build();
```


### Spring - AsyncRestTemplate

**Requirement**: `org.springframework:spring-web` must be provided at runtime.

```java
HermesClient client = HermesClientBuilder.hermesClient(new RestTemplateHermesSender(new AsyncRestTemplate()))
    .withURI(URI.create("http://localhost:8080"))
    .build();
```

### Jersey Client

**Requirement**: `org.glassfish.jersey.core:jersey-client` must be provided at runtime.

```java
HermesClient client = HermesClientBuilder.hermesClient(new JerseyHermesSender(ClientBuilder.newClient()))
    .withURI(URI.create("http://localhost:8080"))
    .build();
```

### OkHttp Client

Requirement: `com.squareup.okhttp:okhttp` must be provided at runtime.

```java
HermesClient client = HermesClientBuilder.hermesClient(new OkHttpHermesSender(new OkHttpClient()))
    .withURI(URI.create("http://localhost:8080"))
    .build();
```

#### HTTP2 support

Requirements:

JVM configured with [ALPN support](http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html#alpn-starting):

```bash
java -Xbootclasspath/p:<path_to_alpn_boot_jar> ...
```

OkHttp Client configured with [SSL support](https://github.com/square/okhttp/wiki/HTTPS):

```java
OkHttpClient okHttpClient = new OkHttpClient();
okHttpClient.setSslSocketFactory(getSslContext().getSocketFactory());
HermesClient client = HermesClientBuilder.hermesClient(new OkHttpHermesSender(okHttpClient))
    .withURI(URI.create("https://localhost:8443"))
    .build();
```

## Publishing

```java
CompletableFuture<HermesResponse> result = client.publish("group.topic", "{}");

HermesResponse response = result.join();

assertThat(response.isSuccess()).isTrue();
assertThat(response.getStatusCode()).isEqualTo(201);
assertThat(response.getMessageId()).isNotEmpty();
```

## Metrics

**Requirement**: dependency `io.dropwizard.metrics:metrics-core` must be provided at runtime.

```java
MetricRegistry registry = myMetricRegistryFactory.createMetricRegistry();

HermesClient client = HermesClientBuilder.hermesClient(sender)
    .withURI(URI.create("http://localhost:8080"))
    .withMetrics(registry)
    .build();
```

## Custom HermesSender

Example with [Unirest](http://unirest.io/java.html) - very simple http client.

```java
HermesClient client = HermesClientBuilder.hermesClient((uri, message) -> {
    CompletableFuture<HermesResponse> future = new CompletableFuture<>();

    Unirest.post(uri.toString()).body(message.getBody()).asStringAsync(new Callback<String>() {
        @Override
        public void completed(HttpResponse<String> response) {
            future.complete(() -> response.getStatus());
        }

        @Override
        public void failed(UnirestException exception) {
            future.completeExceptionally(exception);
        }

        @Override
        public void cancelled() {
            future.cancel(true);
        }
    });

    return future;
})
.withURI(URI.create("http://localhost:8080"))
.build();
```
