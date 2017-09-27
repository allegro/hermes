package pl.allegro.tech.hermes.api.constraints;

public final class Names {
    private static final String ALLOWED_CHARACTERS = "[a-zA-Z0-9_]";
    private static final String DELIMITER = "\\.";

    public static final String ALLOWED_NAME_REGEX = ALLOWED_CHARACTERS + "+";

    public static final String ALLOWED_GROUP_NAME_REGEX =
            ALLOWED_NAME_REGEX + "(" + DELIMITER + ALLOWED_NAME_REGEX + ")*";

    public static final String ALLOWED_TOPIC_NAME_REGEX = ALLOWED_NAME_REGEX;

    public static final String ALLOWED_SUBSCRIPTION_NAME_REGEX = ALLOWED_NAME_REGEX;

}
