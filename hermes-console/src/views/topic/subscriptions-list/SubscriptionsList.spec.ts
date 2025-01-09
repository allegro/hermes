import { beforeEach } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import { createTestingPiniaWithState } from '@/dummy/store';
import { describe, expect } from 'vitest';
import {
  dummyDataSources,
  dummyInitializedSubscriptionForm,
  dummySubscriptionFormValidator,
} from '@/dummy/subscription-form';
import { dummyRoles } from '@/dummy/roles';
import {
  dummySubscription,
  secondDummySubscription,
} from '@/dummy/subscription';
import { fireEvent } from '@testing-library/vue';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import { useCreateSubscription } from '@/composables/subscription/use-create-subscription/useCreateSubscription';
import router from '@/router';
import SubscriptionsList from '@/views/topic/subscriptions-list/SubscriptionsList.vue';
import userEvent from '@testing-library/user-event';
import type { UseCreateSubscription } from '@/composables/subscription/use-create-subscription/types';

vi.mock(
  '@/composables/subscription/use-create-subscription/useCreateSubscription',
);

const useCreateSubscriptionStub: UseCreateSubscription = {
  form: ref(dummyInitializedSubscriptionForm),
  validators: dummySubscriptionFormValidator,
  dataSources: dummyDataSources,
  createOrUpdateSubscription: () => Promise.resolve(true),
  creatingOrUpdatingSubscription: ref(false),
  errors: ref({
    fetchOwners: null,
    fetchOwnerSources: null,
  }),
};

describe('SubscriptionsList', () => {
  const props = {
    groupId: 'pl.allegro',
    topicName: 'pl.allegro.DummyTopic',
    subscriptions: [dummySubscription, secondDummySubscription],
    roles: dummyRoles,
  };

  const pinia = createTestingPinia({
    fakeApp: true,
  });

  beforeEach(async () => {
    setActivePinia(pinia);
    await router.push(`/ui/groups/${props.groupId}/topics/${props.topicName}`);
  });

  it('should render proper heading', () => {
    // when
    const { getByText } = render(SubscriptionsList, { props });

    // then
    expect(getByText('topicView.subscriptions.title (2)')).toBeVisible();
  });

  it.each(props.subscriptions)(
    'should render button %s representing each subscription',
    async (subscription) => {
      // when
      const { getByText } = render(SubscriptionsList, { props });
      await userEvent.click(getByText('topicView.subscriptions.title (2)'));

      // then
      expect(getByText(subscription.name)).toBeVisible();
      expect(getByText(subscription.name).closest('a')).toHaveAttribute(
        'href',
        `${window.location.href}/subscriptions/${subscription.name}`,
      );
    },
  );

  it('should show create subscription dialog on button click', async () => {
    // given
    vi.mocked(useCreateSubscription).mockReturnValueOnce(
      useCreateSubscriptionStub,
    );

    // when
    const { getByText, getAllByText } = render(SubscriptionsList, {
      testPinia: createTestingPiniaWithState(),
      props,
    });
    await fireEvent.click(getByText('topicView.subscriptions.title (2)'));
    await fireEvent.click(getByText('topicView.subscriptions.create'));

    // then
    expect(
      getAllByText('topicView.subscriptions.create')[0],
    ).toBeInTheDocument();
    expect(getByText('subscriptionForm.actions.create')).toBeInTheDocument();
  });

  it.each(['foobar', 'FOOBAR', 'FooBar'])(
    'should render subscription list and apply a filter (case-insensitive, filter: %s)',
    async (filter: string) => {
      // given
      const { getByText, getByLabelText, queryByText } = render(
        SubscriptionsList,
        {
          props,
        },
      );

      // when
      await fireEvent.click(getByText('topicView.subscriptions.title (2)'));

      // then
      expect(queryByText('foobar-service')).toBeInTheDocument();
      expect(queryByText('bazbar-service')).toBeInTheDocument();

      // when
      await fireEvent.update(
        getByLabelText('topicView.subscriptions.search'),
        filter,
      );

      // then
      expect(queryByText('foobar-service')).toBeInTheDocument();
      expect(queryByText('bazbar-service')).not.toBeInTheDocument();
    },
  );

  it('should render copy clients button when there are subscriptions', async () => {
    // when
    const { getByText } = render(SubscriptionsList, { props });
    await fireEvent.click(getByText('topicView.subscriptions.title (2)'));

    // then
    expect(getByText('topicView.subscriptions.copy')).toBeVisible();
  });

  it('should not render copy clients button when there are no subscriptions', async () => {
    // given
    const propsWithoutSubscriptions = {
      groupId: 'pl.allegro',
      topicName: 'pl.allegro.DummyTopic',
      subscriptions: [],
      roles: dummyRoles,
    };

    // when
    const { getByText, queryByText } = render(SubscriptionsList, {
      props: propsWithoutSubscriptions,
    });
    await fireEvent.click(getByText('topicView.subscriptions.title (0)'));

    // then
    expect(queryByText('topicView.subscriptions.copy')).not.toBeInTheDocument();
  });
});
