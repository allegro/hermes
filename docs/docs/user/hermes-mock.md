# Hermes Mock

A thin library designed to mocking Hermes Frontend in tests.

## Overview

Mocking is provided by `HermesMock` class, which consists of 3 parts:
- `HermesMockDefine` 
- `HermesMockExpect`
- `HermesMockQuery`

##### HermesMockDefine

Is responsible for defining new topics on Hermes, provides the following methods:

- `jsonTopic(String topicName)` - defines a JSON topic.

- `avroTopic(String topicName)` - defines an Avro topic.

- `jsonTopic(String topicName, int statusCode)` - defines a JSON topic that when published on responds
with a given response code.

- `avroTopic(String topicName, int statusCode)` - defines an Avro topic that when published on responds
with a given response code.

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
compile group: 'pl.allegro.tech.hermes', name: 'hermes-mock', version: versions.hermes
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
