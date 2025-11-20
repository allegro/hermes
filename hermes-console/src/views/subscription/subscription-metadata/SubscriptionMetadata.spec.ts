import {
  appConfigStoreState,
  createTestingPiniaWithState,
  favoritesStoreState,
} from '@/dummy/store';
import { copyToClipboard } from '@/utils/copy-utils';
import { createTestingPinia } from '@pinia/testing';
import {
  dummyDataSources,
  dummyInitializedSubscriptionForm,
  dummySubscriptionFormValidator,
} from '@/dummy/subscription-form';
import { dummyOwner } from '@/dummy/topic';
import { dummySubscription } from '@/dummy/subscription';
import { expect } from 'vitest';
import { fireEvent } from '@testing-library/vue';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { Role } from '@/api/role';
import { setActivePinia } from 'pinia';
import { State } from '@/api/subscription';
import { useEditSubscription } from '@/composables/subscription/use-edit-subscription/useEditSubscription';
import { useFavorites } from '@/store/favorites/useFavorites';
import SubscriptionMetadata from '@/views/subscription/subscription-metadata/SubscriptionMetadata.vue';
import type { UseEditSubscription } from '@/composables/subscription/use-edit-subscription/types';

vi.mock('@/composables/subscription/use-edit-subscription/useEditSubscription');
vi.mock('@/utils/copy-utils');

const useEditSubscriptionStub: UseEditSubscription = {
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

describe('SubscriptionMetadata', () => {
  it('should render subscription metadata box', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        name: 'subscription-name',
        endpoint: 'service://subscription-name/dummy',
        description: 'some description',
      },
      owner: dummyOwner,
      roles: [Role.SUBSCRIPTION_OWNER],
    };

    // when
    const { getByText } = render(SubscriptionMetadata, { props });

    // then
    expect(
      getByText('subscription.subscriptionMetadata.subscription'),
    ).toBeVisible();
    expect(getByText('subscription-name')).toBeVisible();
    expect(
      getByText(
        'subscription.subscriptionMetadata.endpoint service://subscription-name/dummy',
      ),
    ).toBeVisible();
    expect(getByText('some description')).toBeVisible();
  });

  it('should show add subscription to favorites button', async () => {
    // given
    const props = {
      subscription: dummySubscription,
      owner: dummyOwner,
      roles: [],
    };

    // when
    const { getByText, queryByText } = render(SubscriptionMetadata, {
      props,
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(
      getByText('subscription.subscriptionMetadata.actions.addToFavorites'),
    ).toBeInTheDocument();
    expect(
      queryByText(
        'subscription.subscriptionMetadata.actions.removeFromFavorites',
      ),
    ).not.toBeInTheDocument();
  });

  it('should add subscription to favorites', async () => {
    // given
    const pinia = createTestingPinia({
      createSpy: vi.fn,
      initialState: { favorites: favoritesStoreState },
    });
    setActivePinia(pinia);
    const favoritesStore = useFavorites();
    vi.mocked(favoritesStore.addSubscription).mockReturnValueOnce(
      Promise.resolve(),
    );
    const props = {
      subscription: dummySubscription,
      owner: dummyOwner,
      roles: [],
    };
    const { getByTestId } = render(SubscriptionMetadata, {
      testPinia: pinia,
      props,
    });

    // when
    await fireEvent.click(getByTestId('add-favorite-subscription-button'));

    // then
    expect(favoritesStore.addSubscription).toHaveBeenCalledOnce();
    expect(favoritesStore.addSubscription).toHaveBeenCalledWith(
      'pl.allegro.public.group.DummyEvent',
      'foobar-service',
    );
  });

  it('should show remove subscription from favorites button', async () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        name: 'sub',
        topicName: 'topic',
      },
      owner: dummyOwner,
      roles: [],
    };

    // when
    const { getByText, queryByText } = render(SubscriptionMetadata, {
      props,
      testPinia: createTestingPinia({
        initialState: {
          favorites: {
            subscriptions: ['topic$sub'],
          },
          appConfig: appConfigStoreState,
        },
      }),
    });

    // then
    expect(
      queryByText('subscription.subscriptionMetadata.actions.addToFavorites'),
    ).not.toBeInTheDocument();
    expect(
      getByText(
        'subscription.subscriptionMetadata.actions.removeFromFavorites',
      ),
    ).toBeInTheDocument();
  });

  it('should remove subscription from favorites', async () => {
    // given
    const pinia = createTestingPinia({
      createSpy: vi.fn,
      initialState: {
        favorites: {
          ...favoritesStoreState,
          subscriptions: [
            `${dummySubscription.topicName}$${dummySubscription.name}`,
          ],
        },
        appConfig: appConfigStoreState,
      },
    });
    setActivePinia(pinia);
    const favoritesStore = useFavorites();
    vi.mocked(favoritesStore.addSubscription).mockReturnValueOnce(
      Promise.resolve(),
    );
    const props = {
      subscription: dummySubscription,
      owner: dummyOwner,
      roles: [],
    };
    const { getByTestId } = render(SubscriptionMetadata, {
      testPinia: pinia,
      props,
    });

    // when
    await fireEvent.click(getByTestId('remove-favorite-subscription-button'));

    // then
    expect(favoritesStore.removeSubscription).toHaveBeenCalledOnce();
    expect(favoritesStore.removeSubscription).toHaveBeenCalledWith(
      'pl.allegro.public.group.DummyEvent',
      'foobar-service',
    );
  });

  it('should render owners button', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        owner: {
          ...dummySubscription.owner,
          source: 'some source',
        },
      },
      owner: dummyOwner,
      roles: [Role.SUBSCRIPTION_OWNER],
    };

    // when
    const { getByText } = render(SubscriptionMetadata, { props });

    // then
    expect(getByText('your-super-service')).toBeVisible();
    expect(getByText('your-super-service')).toHaveAttribute(
      'href',
      dummyOwner.url,
    );
  });

  it('should render diagnostics button', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        owner: {
          ...dummySubscription.owner,
          source: 'some source',
        },
      },
      owner: dummyOwner,
      roles: [Role.ADMIN],
    };

    // when
    const { getByText } = render(SubscriptionMetadata, { props });

    // then
    expect(
      getByText('subscription.subscriptionMetadata.actions.diagnostics'),
    ).toBeVisible();
  });

  it('should not render diagnostics button', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        owner: {
          ...dummySubscription.owner,
          source: 'some source',
        },
      },
      owner: dummyOwner,
      roles: [],
    };

    // when
    const { queryByText } = render(SubscriptionMetadata, { props });

    // then
    expect(
      queryByText('subscription.subscriptionMetadata.actions.diagnostics'),
    ).not.toBeInTheDocument();
  });

  it('should disable subscription actions buttons', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        owner: {
          ...dummySubscription.owner,
          source: 'some source',
        },
      },
      owner: dummyOwner,
      roles: [],
    };

    // when
    const { getByText } = render(SubscriptionMetadata, { props });

    // then
    expect(
      getByText('subscription.subscriptionMetadata.actions.suspend').closest(
        'button',
      ),
    ).toBeDisabled();
    expect(
      getByText('subscription.subscriptionMetadata.actions.edit').closest(
        'button',
      ),
    ).toBeDisabled();
    expect(
      getByText('subscription.subscriptionMetadata.actions.remove').closest(
        'button',
      ),
    ).toBeDisabled();
  });

  it('should render "activate" button and hide "suspend" if subscription is suspended', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        state: State.SUSPENDED,
      },
      owner: dummyOwner,
      roles: [Role.SUBSCRIPTION_OWNER],
    };

    // when
    const { queryByText } = render(SubscriptionMetadata, { props });

    // then
    expect(
      queryByText('subscription.subscriptionMetadata.actions.activate'),
    ).toBeVisible();
    expect(
      queryByText('subscription.subscriptionMetadata.actions.suspend'),
    ).not.toBeInTheDocument();
  });

  it('should render "suspend" button and hide "activate" if subscription is active', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        state: State.ACTIVE,
      },
      owner: dummyOwner,
      roles: [Role.SUBSCRIPTION_OWNER],
    };

    // when
    const { queryByText } = render(SubscriptionMetadata, { props });

    // then
    expect(
      queryByText('subscription.subscriptionMetadata.actions.activate'),
    ).not.toBeInTheDocument();
    expect(
      queryByText('subscription.subscriptionMetadata.actions.suspend'),
    ).toBeVisible();
  });

  it('should show edit subscription dialog on button click', async () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        name: 'subscription-name',
        endpoint: 'service://subscription-name/dummy',
        description: 'some description',
      },
      owner: dummyOwner,
      roles: [Role.SUBSCRIPTION_OWNER, Role.ADMIN],
    };
    vi.mocked(useEditSubscription).mockReturnValueOnce(useEditSubscriptionStub);

    // when
    const { getByText } = render(SubscriptionMetadata, {
      testPinia: createTestingPiniaWithState(),
      props,
    });
    await fireEvent.click(
      getByText('subscription.subscriptionMetadata.actions.edit'),
    );

    // then
    expect(
      getByText('subscription.subscriptionMetadata.editSubscription'),
    ).toBeInTheDocument();
    expect(getByText('subscriptionForm.actions.update')).toBeInTheDocument();
  });

  it('should copy subscription name to clipboard on button click', async () => {
    // given
    const props = {
      subscription: dummySubscription,
      owner: dummyOwner,
      roles: [],
    };
    const { getByTestId } = render(SubscriptionMetadata, {
      props,
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await fireEvent.click(getByTestId('copy-subscription-name-button'));

    // then
    expect(copyToClipboard).toHaveBeenCalledOnce();
    expect(copyToClipboard).toHaveBeenCalledWith(
      'pl.allegro.public.group.DummyEvent$foobar-service',
    );
  });
});
