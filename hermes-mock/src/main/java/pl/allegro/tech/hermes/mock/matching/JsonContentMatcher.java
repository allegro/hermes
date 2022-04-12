package pl.allegro.tech.hermes.mock.matching;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;

import pl.allegro.tech.hermes.mock.HermesMockHelper;

import java.util.function.Predicate;

public class JsonContentMatcher<T> implements ValueMatcher<Request> {

    private final Predicate<T> predicate;
    private final Class<T> clazz;
    private final HermesMockHelper hermesMockHelper;

    public JsonContentMatcher(HermesMockHelper hermesMockHelper, Predicate<T> predicate , Class<T> clazz) {
        this.hermesMockHelper = hermesMockHelper;
        this.predicate = predicate;
        this.clazz = clazz;
    }

    @Override
    public MatchResult match(Request actual) {
        T body = this.hermesMockHelper.deserializeJson(actual, clazz);

        return MatchResult.of(predicate.test(body));
    }
}