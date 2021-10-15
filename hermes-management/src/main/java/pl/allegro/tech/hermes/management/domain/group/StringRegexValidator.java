package pl.allegro.tech.hermes.management.domain.group;

import java.util.regex.Pattern;

class StringRegexValidator {

    private final Pattern allowedPattern;

    public StringRegexValidator(String allowedRegex) {
        this.allowedPattern = Pattern.compile(allowedRegex);
    }

    public boolean isValid(String groupName) {
        return allowedPattern.matcher(groupName).matches();
    }
}
