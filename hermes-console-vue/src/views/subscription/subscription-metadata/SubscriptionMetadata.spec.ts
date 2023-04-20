import { dummySubscription } from '@/dummy/subscription';
import { render } from '@/utils/test-utils';
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
      authorized: true,
    };

    // when
    const { getByText } = render(SubscriptionMetadata, { props });

    // then
    expect(
      getByText('subscription.subscriptionMetadata.subscription'),
    ).toBeInTheDocument();
    expect(getByText('subscription-name')).toBeInTheDocument();
    expect(getByText('service://subscription-name/dummy')).toBeInTheDocument();
    expect(getByText('some description')).toBeInTheDocument();
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
      authorized: true,
    };

    // when
    const { getByText } = render(SubscriptionMetadata, { props });

    // then
    expect(
      getByText('subscription.subscriptionMetadata.owners (some source)'),
    ).toBeInTheDocument();
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
      authorized: true,
    };

    // when
    const { getByText } = render(SubscriptionMetadata, { props });

    // then
    expect(
      getByText('subscription.subscriptionMetadata.actions.diagnostics'),
    ).toBeInTheDocument();
  });

  it('should render "activate" button and hide "suspend" if subscription is suspended', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        state: State.SUSPENDED,
      },
      authorized: false,
    };

    // when
    const { queryByText } = render(SubscriptionMetadata, { props });

    // then
    expect(
      queryByText('subscription.subscriptionMetadata.actions.activate'),
    ).toBeInTheDocument();
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
      authorized: false,
    };

    // when
    const { queryByText } = render(SubscriptionMetadata, { props });

    // then
    expect(
      queryByText('subscription.subscriptionMetadata.actions.activate'),
    ).not.toBeInTheDocument();
    expect(
      queryByText('subscription.subscriptionMetadata.actions.suspend'),
    ).toBeInTheDocument();
  });
});
