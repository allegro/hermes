Java Client
===========

A thin library designed to publish messages to Hermes.

Features
--------

* http client-agnostic API
* synchronous/asynchronous publishing
* configurable retries
* metrics

Overview
--------

Core functionality is provided by *HermesClient* class. *HermesClient* is built on top of http client.
Which http client is in use depends on implementation of *HermesSender* interface provided to *HermesClient*.

At the moment, there are 3 ready to use implementations of *HermesSender*:

* *RestTemplateHermesSender* - recommended for services built on `Spring framework <http://projects.spring.io/spring-framework>`_.
  Uses `AsyncRestTemplate <http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/AsyncRestTemplate.html>`_
* *JerseyHermesSender* - recommended for services built on `Jersey framework <https://jersey.java.net/>`_. Uses `Jersey client <https://jersey.java.net/documentation/latest/client.html>`_
* *OkHttpHermesSender* - supports both HTTP/1.1 and HTTP/2 protocols, uses `OkHttp client <http://square.github.io/okhttp/>`_

Custom implementation of *HermesSender* also can be provided as well.

Getting started
---------------

Gradle
^^^^^^
.. parsed-literal::

    dependencies {
        compile group: 'pl.allegro.tech.hermes', name: 'hermes-client', version: '|version|'
    }

Initialization
^^^^^^^^^^^^^^

Spring - AsyncRestTemplate
''''''''''''''''''''''''''

Requirement: dependency ``org.springframework:spring-web`` must be provided at runtime.

.. code-block:: java

    HermesClient client = HermesClientBuilder.hermesClient(new RestTemplateHermesSender(new AsyncRestTemplate()))
        .withURI(URI.create("http://localhost:8080"))
        .build();

Jersey Client
'''''''''''''

Requirement: dependency ``org.glassfish.jersey.core:jersey-client`` must be provided at runtime.

.. code-block:: java

    HermesClient client = HermesClientBuilder.hermesClient(new JerseyHermesSender(ClientBuilder.newClient()))
        .withURI(URI.create("http://localhost:8080"))
        .build();

OkHttp Client
'''''''''''''

Requirement: dependency ``com.squareup.okhttp:okhttp`` must be provided at runtime.

.. code-block:: java

    HermesClient client = HermesClientBuilder.hermesClient(new OkHttpHermesSender(new OkHttpClient()))
        .withURI(URI.create("http://localhost:8080"))
        .build();

HTTP2 support:
++++++++++++++
Requirements:

JVM configured with `ALPN support <http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html#alpn-starting>`_:

.. code-block::

    java -Xbootclasspath/p:<path_to_alpn_boot_jar> ...

OkHttp Client configured with SSL support (`OkHttp Wiki <https://github.com/square/okhttp/wiki/HTTPS>`_):

.. code-block:: java

    OkHttpClient okHttpClient = new OkHttpClient();
    okHttpClient.setSslSocketFactory(getSslContext().getSocketFactory());
    HermesClient client = HermesClientBuilder.hermesClient(new OkHttpHermesSender(okHttpClient))
        .withURI(URI.create("https://localhost:8443"))
        .build();

..

Publishing
^^^^^^^^^^

.. code-block:: java

    CompletableFuture<HermesResponse> result = client.publish("group.topic", "{}");

    HermesResponse response = result.join();

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getStatusCode()).isEqualTo(201);
    assertThat(response.getMessageId()).isNotEmpty();

Customization
^^^^^^^^^^^^^

Enabling Dropwizard metrics
'''''''''''''''''''''''''''

Requirement: dependency ``io.dropwizard.metrics:metrics-core`` must be provided at runtime.

.. code-block:: java

    HermesClient client = HermesClientBuilder.hermesClient(sender)
        .withURI(URI.create("http://localhost:8080"))
        .withMetrics(new MetricRegistry())
        .build();

Custom HermesSender
'''''''''''''''''''

Example with `Unirest <http://unirest.io/java.html>`_ - very simple http client.

.. code-block:: java

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
