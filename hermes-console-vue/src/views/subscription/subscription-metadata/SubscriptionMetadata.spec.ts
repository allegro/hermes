import { beforeEach } from 'vitest';
import { dummySubscription } from '@/dummy/subscription';
import { expect } from 'vitest';
import { render } from '@/utils/test-utils';
import { Role } from '@/api/role';
import { State } from '@/api/subscription';
import router from '@/router';
import SubscriptionMetadata from '@/views/subscription/subscription-metadata/SubscriptionMetadata.vue';

describe('SubscriptionMetadata', () => {
  beforeEach(async () => {
    await router.push(
      '/ui/groups/pl.allegro.public.group' +
        '/topics/pl.allegro.public.group.DummyEvent' +
        '/subscriptions/foobar-service',
    );
  });

  it('should render subscription metadata box', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        name: 'subscription-name',
        endpoint: 'service://subscription-name/dummy',
        description: 'some description',
      },
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
      roles: [Role.SUBSCRIPTION_OWNER],
    };

    // when
    const { getByText } = render(SubscriptionMetadata, { props });

    // then
    expect(
      getByText('subscription.subscriptionMetadata.owners (some source)'),
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
