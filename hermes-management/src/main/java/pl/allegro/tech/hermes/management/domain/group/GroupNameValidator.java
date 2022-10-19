package pl.allegro.tech.hermes.management.domain.group;

import pl.allegro.tech.hermes.management.domain.GroupNameIsNotAllowedException;

import java.util.regex.Pattern;

class GroupNameValidator {

    private final Pattern allowedPattern;

    public GroupNameValidator(String allowedRegex) {
        this.allowedPattern = Pattern.compile(allowedRegex);
    }

    public void requireValid(String groupName) {
        if (!allowedPattern.matcher(groupName).matches()) {
            throw new GroupNameIsNotAllowedException(String.format("Group name should match pattern %s", allowedPattern));
        }
    }
}
