package pl.allegro.tech.hermes.mock

import groovy.json.JsonOutput

class TestMessage {
    String key
    String value

    TestMessage() {}

    TestMessage(String key, Object value) {
        this.key = key
        this.value = value
    }

    static TestMessage random() {
        return new TestMessage("random", UUID.randomUUID().toString())
    }

    String asJson() {
        JsonOutput.toJson(this)
    }
}

class TestMessageWithDifferentSchema {
     String key
     List<Integer> value

    TestMessageWithDifferentSchema() {}

    TestMessageWithDifferentSchema(String key, Integer value) {
        this.key = key
        this.value = List.of(value)
    }

    static TestMessageWithDifferentSchema random() {
        return new TestMessageWithDifferentSchema("random", Math.random().toInteger())
    }

    String asJson() {
        JsonOutput.toJson(this)
    }
}
