import {
  appConfigStoreState,
  createTestingPiniaWithState,
} from '@/dummy/store';
import { createTestingPinia } from '@pinia/testing';
import { dummyOwner } from '@/dummy/topic';
import { dummySubscription } from '@/dummy/subscription';
import { expect } from 'vitest';
import { render } from '@/utils/test-utils';
import { Role } from '@/api/role';
import { State } from '@/api/subscription';
import SubscriptionMetadata from '@/views/subscription/subscription-metadata/SubscriptionMetadata.vue';

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
    expect(getByText('service://subscription-name/dummy')).toBeVisible();
    expect(getByText('some description')).toBeVisible();
  });

  it('should show add subscription to favorites button', async () => {
    // given
    const props = {
      subscription: {
        dummySubscription,
      },
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
    expect(
      getByText('subscription.subscriptionMetadata.owners your-super-service'),
    ).toBeVisible();
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
      getByText('subscription.subscriptionMetadata.actions.clone').closest(
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
});
