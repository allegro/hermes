package pl.allegro.tech.hermes.api.constraints;

import com.google.common.base.Objects;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MessageFiltersValidator extends BaseValidator implements ConstraintValidator<ValidMessageFilters, Subscription> {

    private static final String ERROR_MESSAGE = "Message filter type %s doesn't match subscription content type %s";

    private static final Set<ContentTypeFilterTypePair> VALID_TYPES_COMBINATIONS = new HashSet<>(Arrays.asList(
            new ContentTypeFilterTypePair(ContentType.JSON, "jsonpath"),
            new ContentTypeFilterTypePair(ContentType.JSON, "header"),
            new ContentTypeFilterTypePair(ContentType.AVRO, "avropath"),
            new ContentTypeFilterTypePair(ContentType.AVRO, "header")
            ));

    @Override
    public void initialize(ValidMessageFilters constraintAnnotation) {

    }

    @Override
    public boolean isValid(Subscription subscription, ConstraintValidatorContext context) {
        return subscription.getFilters()
                .stream()
                .map(filter -> new ContentTypeFilterTypePair(subscription.getContentType(), filter.getType()))
                .filter(pair -> notMatchContentType(pair, context))
                .count() == 0;

    }

    private boolean notMatchContentType(ContentTypeFilterTypePair pair, ConstraintValidatorContext context) {
        if (!VALID_TYPES_COMBINATIONS.contains(pair)){
            createConstraintMessage(context, String.format(ERROR_MESSAGE, pair.filterType, pair.contentType));
            return true;
        } else {
            return false;
        }
    }

    static class ContentTypeFilterTypePair {
        private ContentType contentType;
        private String filterType;

        ContentTypeFilterTypePair(ContentType contentType, String filterType) {
            this.contentType = contentType;
            this.filterType = filterType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ContentTypeFilterTypePair validPair = (ContentTypeFilterTypePair) o;
            return contentType == validPair.contentType &&
                    Objects.equal(filterType, validPair.filterType);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(contentType, filterType);
        }
    }

}
