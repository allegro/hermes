# Packaging

Hermes was built with extensibility in mind. As a result, there are two ways to build deployment packages.

## Vanilla Hermes

This is the simplest way to start. Clone the repository and build distribution packages using [Gradle](http://gradle.org).

```bash
git clone https://github.com/allegro/hermes.git

# build hermes
cd hermes
./gradlew distZip -Pdistribution
```

This will cause distribution packages for all modules to build. Look for them in:

* hermes-frontend/build/distribution/hermes-frontend-{version}.zip
* hermes-consumers/build/distribution/hermes-consumers-{version}.zip
* hermes-management/build/distribution/hermes-management-{version}.zip

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

The Frontend module is a [Spring Boot](https://spring.io/projects/spring-boot/) project. Thus, it can be extended
like any other Spring application.

```java
@ComponentScan(
        basePackages = {"pl.allegro.tech.hermes.frontend", "com.example.my-hermes.frontend"},
        excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = HermesFrontend.class)}
)
@SpringBootApplication
public class MyHermesFrontend {

    public static void main(String... args) {
        SpringApplication.run(MyHermesFrontend.class, args);
    }
    
}
```

Create any type of runnable (*distZip* or *fatJar*) and deploy it.

### Consumers

The Consumers module is a [Spring Boot](https://spring.io/projects/spring-boot/) project. Thus, it can be extended
like any other Spring application.

```groovy
compile group: 'pl.allegro.tech.hermes', name: 'hermes-consumers', version: versions.hermes
```

```java
@ComponentScan(
        basePackages = {"pl.allegro.tech.hermes.consumers", "com.example.my-hermes.consumers"},
        excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = HermesConsumers.class)}
)
@SpringBootApplication
public class MyHermesConsumers {

    public static void main(String... args) {
        SpringApplication.run(MyHermesConsumers.class, args);
    }

}
```

Create any type of runnable (*distZip* or *fatJar*) and deploy it.

### Management

Hermes management is a simple [Spring Boot](https://spring.io/projects/spring-boot/) project. Thus, it can be extended
like any other Spring application.

```groovy
compile group: 'pl.allegro.tech.hermes', name: 'hermes-management', version: versions.hermes
```

```java
@ComponentScan(
        basePackages = {"pl.allegro.tech.hermes.management", "com.example.my-hermes.management"},
        excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = HermesManagement.class)}
)
@SpringBootApplication
public class MyHermesManagement {

    public static void main(String... args) {
        SpringApplication.run(MyHermesManagement.class, args);
    }

}
```
