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
