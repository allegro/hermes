Management module
=================

Management module contains simple Spring Boot application that exposes REST interface for management. It should
be included as dependency in any Spring Boot application - this way you can provide own security for endpoints or compose
it with your in-company Spring Boot stack.

Currently Hermes management endpoints are served using Jersey. We plan on migrating it to Spring MVC in incoming releases.

Basic usage
-----------

In your ``build.gradle`` add::

    repositories {
        mavenCentral()
    }

    dependencies {
        compile group: 'pl.allegro.tech.hermes', name: 'hermes-management', version: 'hermes version'
    }

To enable Hermes endpoints, just add component scan for ``pl.allegro.tech.hermes.management`` package, for example::

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"pl.allegro.tech.hermes.management", "com.mycompany.mypackages"})
    public class MyCompanyHermesManagement {

        public static void main(String... args) {
            SpringApplication.run(MyCompanyHermesManagement.class, args);
        }
    }


Configuration
-------------

Since management is based on Spring Boot, we use
`default configuration mechanisms <http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html>`_.

Kafka broker
^^^^^^^^^^^^

======================== ========================================== ================
Property                 Description                                Default value
======================== ========================================== ================
kafka.connectionString   connection string to Kafka Zookeeper       localhost:2181
kafka.sessionTimeout     Zookeeper session timeout                  10000
kafka.connectionTimeout  Zookeeper connection timeout               1000
kafka.retryTimes         how many times retry Zookeeper conneciton  3
kafka.retrySleep         sleep period before consequent retries     1000
======================== ========================================== ================

Kafka topics properties
^^^^^^^^^^^^^^^^^^^^^^^

======================== ==================================== ================
Property                 Description                          Default value
======================== ==================================== ================
topic.replicationFactor  replication factor to use            1
topic.partitions         number of partitions for topic       10
topic.allowRemoval       should API allow on removing topics  false
======================== ==================================== ================

Storage Zookeeper
^^^^^^^^^^^^^^^^^

Properties to configure Zookeeper in which Hermes data is kept. For simple usecases (dev/test) this might be the same
as Kafka Zookeeper.

========================= ========================================== =================
Property                  Description                                Default value
========================= ========================================== =================
storage.prefix            prefix under which data will be kept       /hermes
storage.connectionString  connection string to Kafka Zookeeper       localhost:2181
storage.sessionTimeout    Zookeeper session timeout                  10000
storage.connectTimeout    Zookeeper connection timeout               1000
storage.retryTimes        how many times retry Zookeeper conneciton  3
storage.retrySleep        sleep period before consequent retries     1000
========================= ========================================== =================

Subscription
^^^^^^^^^^^^

========================================= ======================================================================== =================
Property                                    Description                                                              Default value
========================================= ======================================================================== =================
subscription.additionalEndpointProtocols  list of protocols, which should be treated as valid (e.x. service, ems)  <empty>
========================================= ======================================================================== =================

Metrics (graphite) access
^^^^^^^^^^^^^^^^^^^^^^^^^

======================== ==================================== =================
Property                  Description                         Default value
======================== ==================================== =================
metrics.graphiteHttpUri  uri to Graphite Web interface        localhost
metrics.prefix           prefix to hermes stats in Graphite   stats.tech.hermes
======================== ==================================== =================

Authorization
-------------

User roles
^^^^^^^^^^

Before reading this section, take a look at :doc:`/contents/overview/02_access_model`.

There are 4 roles defined on management endpoints, listed in ``Roles`` class:

* none (no role) - for all Read-Only operations
* **ADMIN** - has access to all operations on all entities, can create new Groups
* **GROUP_OWNER** - has access to all operations in scope of owned group (including subscription management)
* **SUBSCRIPTION_OWNER** - has access to all operations in scope of owned subscriptions, potentially accross multiple groups

Default implementation of authorization filter does not enforce any access restrictions.

Securing management
^^^^^^^^^^^^^^^^^^^

Security is based on `JSR 250 <https://jcp.org/en/jsr/detail?id=250>`_ annotations. Security is provided via Jersey ``SecurityContext``
class.

To override default allow-all rules, you should create own implementation of ``pl.allegro.tech.hermes.management.api.auth.SecurityContextProvider``
and make it available as component in Spring context::

    @Component
    public class MyCustomSecurityContextProvider implements SecurityContextProvider {

        @Override
        public SecurityContext securityContext(ContainerRequestContext requestContext) {
            Strign username = extractUserFromRequest(requestContext);

            return new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    /* ... */
                }

                @Override
                public boolean isUserInRole(String role) {
                    return myAuthorizationRepository.isUserInRole(username, role);
                }

                @Override
                public boolean isSecure() {
                    /* ... */
                }

                @Override
                public String getAuthenticationScheme() {
                    /* ... */
                }
            };
        }
    }

This will be called on each request to authenticate current user based on your security requirements.
