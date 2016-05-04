# Packaging

Hermes was built with extensibility in mind. As a result, there are two ways to build deployment packages.

## Vanilla Hermes

This is the simplest way to start. Clone the repository and build distribution packages using [Gradle](http://gradle.org).

```bash
git clone https://github.com/allegro/hermes.git

# build hermes
cd hermes
./gradlew distZip -Pdistribution

# build hermes-console
cd hermes-console
./package.sh
```

This will cause distribution packages for all modules to build. Look for them in:

* hermes-frontend/build/distribution/hermes-frontend-{version}.zip
* hermes-consumers/build/distribution/hermes-consumers-{version}.zip
* hermes-management/build/distribution/hermes-management-{version}.zip
* hermes-console/dist/hermes-console.zip

Having deployed Vanilla Hermes, you can only use configuration options to alter the behavior.

## Custom Hermes

Not all features can be configured via configuration options. Some require adding additional code to Hermes (like startup
or shutdown hooks, authorization etc). This is why Hermes modules are published in [Maven Central](http://maven.org) and
can be added as a dependency to any application. This is how we extend and add additional features to our internal Hermes
deployment.

### Frontend

Add dependency on Frontend module:

```groovy
compile group: 'pl.allegro.tech.hermes', name: 'hermes-frontend', version: versions.hermes
```

Use `HermesFrontend.Builder` to create usable `HermesFrontend` instance and start it:

```java
public class MyHermesFrontend {

    private static final Logger logger = LoggerFactory.logger(MyHermesFrontend.class);

    public static final void main(String... args) {
        HermesFrontend.Builder builder = HermesFrontend.frontend()
            .withStartupHook((serviceLocator) -> logger.info("Starting MyHermes"))
            .withShutdownHook((serviceLocator) -> logger.info("Stopping MyHermes"));
        /* introduce additional configuration - brokerListeners etc */

        HermesFrontend frontend = builder.build();
        frontend.start();
    }
}
```

Create any type of runnable (*distZip* or *fatJar*) and deploy it.

### Consumers

Add dependency on Consumers module:

```groovy
compile group: 'pl.allegro.tech.hermes', name: 'hermes-consumers', version: versions.hermes
```

Use `HermesConsumersBuilder` to create usable `HermesConsumers` instance and start it:

```java
public class MyHermesConsumers {

    private static final Logger logger = LoggerFactory.logger(MyHermesConsumers.class);

    public static final void main(String... args) {
        HermesConsumersBuilder builder = HermesConsumers.consumers()
            .withStartupHook((serviceLocator) -> logger.info("Starting MyHermes"))
            .withShutdownHook((serviceLocator) -> logger.info("Stopping MyHermes"));
        /* introduce additional configuration like message senders etc */

        HermesConsumers consumers = builder.build();
        consumers.start();
    }
}
```

Create any type of runnable (*distZip* or *fatJar*) and deploy it.

### Management

Hermes management is a simple [Spring Boot](http://projects.spring.io/spring-boot/) project. Thus, it can be extended
like any other Spring application.

```groovy
compile group: 'pl.allegro.tech.hermes', name: 'hermes-management', version: versions.hermes
```

```java
@Configuration
@ComponentScan(
        basePackages = {"pl.allegro.tech.hermes.management", "com.example.my-hermes.management"},
        excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = HermesManagement.class)}
)
public class MyHermesManagement {

    public static void main(String... args) {
        SpringApplication.run(MyHermesManagement.class, args);
    }

}
```
