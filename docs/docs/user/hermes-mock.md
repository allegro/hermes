# Hermes Mock

A thin library designed to mocking Hermes Frontend in tests.

## Overview

Mocking is provided by `HermesMock` class, which consists of 3 parts:
- `HermesMockDefine` 
- `HermesMockExpect`
- `HermesMockQuery`

##### HermesMock

- `<T> void resetReceivedAvroRequests(String topicName, Schema schema, Class<T> clazz, Predicate<T> predicate)` - resets requests received by Avro topic matching given predicate

- `<T> void resetReceivedJsonRequests(String topicName, Class<T> clazz, Predicate<T> predicate)` - resets requests received by Json topic matching given predicate


##### HermesMockDefine

Is responsible for defining new topics on Hermes, provides the following methods.
Both `jsonTopic` and `avroTopic` methods return an StubMapping object: 

- `jsonTopic(String topicName)` - defines a JSON topic.

- `avroTopic(String topicName)` - defines an Avro topic.

- `jsonTopic(String topicName, int statusCode)` - defines a JSON topic that when published on responds
with a given response code.

- `avroTopic(String topicName, int statusCode)` - defines an Avro topic that when published on responds
with a given response code.

- `jsonTopic(String topicName, Response response)` - defines a JSON topic that when published on responds
  with a given response.

- `avroTopic(String topicName, Response response)` - defines an Avro topic that when published on responds
  with a given response.
- `avroTopic(String topicName, Response response, Schema schema, Class<T> clazz, Predicate<T> predicate)` - defines an 
Avro topic with predicate to match request by field in schema
- `jsonTopic(String topicName, Response response, Class<T> clazz, Predicate<T> predicate)` - defines a
 Json topic with predicate to match request by field

- `removeStubMapping(StubMapping stubMapping)` - removes defined stub mapping 

`Response` allows to define the following elements:
- `statusCode` - a HTTP response code
- `fixedDelay` - a response will be returned after the given time

##### HermesMockExpect

Is responsible for expectation of message on Hermes side, provides the following methods:
 
- `void singleMessageOnTopic(String topicName)` - expects 1 and only 1 message on topic.

- `<T> void singleJsonMessageOnTopicAs(String topicName, Class<T> clazz)` - expects 1 and only 1 JSON message on a topic
of a specific type.

- `<T> void singleAvroMessageOnTopic(String topicName, Schema schema)` - expects 1 and only 1 Avro message on a topic 
of a specific type.

- `void messagesOnTopic(String topicName, int count)` - expects particular number of messages on a given topic.

- `<T> void jsonMessagesOnTopicAs(String topicName, int count, Class<T> clazz)` - expects particular number of
JSON messages on a given topic.

- `<T> void avroMessagesOnTopic(String topicName, int count, Schema schema)` - expects particular number of
Avro messages on a given topic.

##### HermesMockQuery

Is responsible for querying Hermes for a received messages, provides the following methods: 

- `List<Request> allRequests()` - get all the received messages.

- `List<Request> allRequestsOnTopic(String topicName)` - get all the received messages on a given topic.

- `<T> List<T> allJsonMessagesAs(String topicName, Class<T> clazz)` - get all the received messages on a given topic.

- `List<byte[]> allAvroRawMessages(String topicName)` - get all the received raw Avro messages on a given topic.

- `<T> List<T> allAvroMessagesAs(String topicName, Schema schema, Class<T> clazz)` - get all the received Avro messages
on a given topic as a specific type.

- `Optional<Request> lastRequest(String topicName)` - get last received request on topic.

- `<T> Optional<T> lastJsonMessageAs(String topicName, Class<T> clazz)` - get last received JSON message on topic
as a specific type.

- `<T> Optional<byte[]> lastAvroRawMessage(String topicName)` - get last received raw Avro message on topic.

- `<T> Optional<T> lastAvroMessageAs(String topicName, Schema schema, Class<T> clazz)` - get last received Avro message
 on topic as a specific type.

## Creating

To start using Hermes mock, add it as a dependency:

```groovy
testImplementation group: 'pl.allegro.tech.hermes', name: 'hermes-mock', version: versions.hermes
```

## Example

```java
class MyServiceTest {

    @Rule
    HermesMockRule hermesMock = new HermesMockRule(8090);
    
    @Test
    public void exampleTest() {
        // given
        MyMessage myMessage = new MyMessage("id123", "content");
        String topicName = "myTopic";
        
        hermesMock.define().jsonTopic(topicName);

        // when
        myService.publishHermesMessage(myMessage);

        // then
        hermesMock.expect().singleJsonMessageOnTopicAs(topicName, MyMessage.class);
        
        // and
        List<MyMessage> all = hermesMock.query().allJsonMessagesAs(topicName, MyMessage.class);
        
        // and verify that `all` contains what we're expecting 
    }
}
```

## JUnit5 / Spock2 automatic startup issues

If you're using JUnit5 or Spock2 `HermesMockRule` won't start automatically in your tests. In order to make it work you 
can:

- Start and stop Hermes mock manually using `HermesMock`:
```java
class Junit5Test {
    
    private HermesMock hermesMock = new HermesMock.Builder().withPort(8090).build();
    
    @BeforeAll
    static void setup() {
        hermesMock.start();
    }

    @AfterAll
    static void cleanup() {
        hermesMock.stop();
    }
    
    @Test
    public void exampleTest() {
        // you can now use Hermes mock as in previous example
    }
}
```

- *(Only for Spock2)* add `spock-junit4` dependency which allows usage of JUnit4 annotations in Spock2, so you can use
`HermesMockRule` as shown earlier.

```groovy
testImplementation group: 'org.spockframework', name: 'spock-junit4', version: versions.spock
```
