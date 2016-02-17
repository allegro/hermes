package pl.allegro.tech.hermes.management.infrastructure.query.parser;

import java.util.Arrays;
import java.util.Optional;

public enum Operator {

    EQ("eq"),
    NE("ne"),
    LIKE("like"),
    IN("in"),

    NOT("not"),
    AND("and"),
    OR("or");

    private String name;

    Operator(String name) {
        this.name = name;
    }

    public static Operator from(String name) {
        return fromOptional(name)
                .orElseThrow(
                        () -> new IllegalArgumentException(String.format("No operator matching '%s' could be found", name))
                );
    }

    public static boolean isValid(String name) {
        return fromOptional(name).isPresent();
    }

    private static Optional<Operator> fromOptional(String name) {
        return Arrays.stream(values())
                .filter(value -> value.name.equalsIgnoreCase(name))
                .findFirst();
    }
}
