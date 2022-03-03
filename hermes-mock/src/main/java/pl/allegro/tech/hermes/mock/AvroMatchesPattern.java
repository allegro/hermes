package pl.allegro.tech.hermes.mock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.matching.BinaryEqualToPattern;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.google.common.io.BaseEncoding;
import org.apache.avro.Schema;

import java.util.function.Predicate;

class AvroMatchesPattern<T> extends BinaryEqualToPattern {

    private final Predicate<T> predicate;
    private final Schema schema;
    private final Class<T> clazz;

    public AvroMatchesPattern(Predicate<T> predicate, Schema schema, Class<T> clazz) {
        super("empty".getBytes());
        this.predicate = predicate;
        this.schema = schema;
        this.clazz = clazz;
    }

    @Override
    public MatchResult match(byte[] actual) {
        HermesMockHelper hermesMockHelper = new HermesMockHelper(null, new ObjectMapper().findAndRegisterModules());
        T body = hermesMockHelper.deserializeAvro(actual, schema, clazz);

        return MatchResult.of(predicate.test(body));
    }

    @Override
    @JsonIgnore
    public String getName() {
        return "binaryMatches";
    }

    @Override
    @JsonIgnore
    public String getExpected() {
        return BaseEncoding.base64().encode(expectedValue);
    }

    @Override
    public String toString() {
        return getName() + " " + getExpected();
    }
}