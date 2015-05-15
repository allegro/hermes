package pl.allegro.tech.hermes.api.helper;

import org.junit.Test;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.helpers.Patch;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;

public class PatchTest {

    @Test
    public void shouldApplyObject() {
        // given
        SubscriptionPolicy policy1 = subscriptionPolicy().applyDefaults().build();
        SubscriptionPolicy policy2 = subscriptionPolicy().withRate(10).withMessageTtl(30).build();
        
        SubscriptionPolicy expectedPolicy = subscriptionPolicy().applyDefaults().withRate(10).withMessageTtl(30).build();

        // when & then
        assertThat(Patch.apply(policy1, policy2)).isEqualTo(expectedPolicy);
    }

    @Test
    public void shouldNotOverrideFieldsWhichWhereNotSupposedToBePatched() {
        //given
        SubscriptionPolicy policy = subscriptionPolicy().applyDefaults().build();

        //when
        SubscriptionPolicy patched =  Patch.apply(policy, subscriptionPolicy().build());

        //then
        assertThat(patched).isEqualTo(policy);
    }

    @Test
    public void shouldApplyPatchToFieldsWhichWhereMeantToBePatched() {
        //given
        SubscriptionPolicy policy = subscriptionPolicy().withRate(10).build();
        SubscriptionPolicy changes = subscriptionPolicy().withRate(8).withMessageTtl(2).build();

        //when
        SubscriptionPolicy patched =  Patch.apply(policy, changes);

        //then
        assertThat(patched.getRate()).isEqualTo(changes.getRate());
        assertThat(patched.getMessageTtl()).isEqualTo(changes.getMessageTtl());
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionForNullValues() {
        //when
        Patch.apply(null, null);

        //then
        //exception is thrown
    }

    @Test
    public void shouldApplyPatchesEvenForDifferentClassHierarchies() {
        //given
        SubscriptionPolicy policy = subscriptionPolicy().withRate(10).build();
        SubscriptionPolicy changes = subscriptionPolicy().withRate(8).build();

        //when
        SubscriptionPolicy patched = Patch.apply(policy, changes);

        //then
        assertThat(patched.getRate()).isEqualTo(changes.getRate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfClassesFieldNamesAreIncompatible() {
        //given
        SubscriptionPolicy policy = subscriptionPolicy().withRate(10).build();
        ErrorDescription changes = new ErrorDescription("foobar", ErrorCode.INTERNAL_ERROR);

        //when
        Patch.apply(policy, changes);
    }

    @Test
    public void shouldPatchNestedObjects() {
        //given
        Subscription subscription = subscription().applyDefaults().build();
        SubscriptionPolicy patch = subscriptionPolicy().withMessageTtl(8).withRate(200).build();

        //when
        SubscriptionPolicy result = Patch.apply(subscription, subscription().withSubscriptionPolicy(patch).build()).getSubscriptionPolicy();

        //then
        assertThat(result.getMessageTtl()).isEqualTo(patch.getMessageTtl());
        assertThat(result.getRate()).isEqualTo(patch.getRate());
    }
}
