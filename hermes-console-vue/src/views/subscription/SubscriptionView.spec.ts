import { beforeEach } from 'vitest';
import { computed, ref } from 'vue';
import {
  dummySubscription,
  dummySubscriptionHealth,
  dummySubscriptionMetrics,
  dummyUndeliveredMessage,
  dummyUndeliveredMessages,
} from '@/dummy/subscription';
import { render } from '@/utils/test-utils';
import { useSubscription } from '@/composables/use-subscription/useSubscription';
import router from '@/router';
import SubscriptionView from '@/views/subscription/SubscriptionView.vue';

vitest.mock('@/composables/use-subscription/useSubscription');
const mockedUseSubscription = vitest.mocked(useSubscription);

const useSubscriptionStub: ReturnType<typeof useSubscription> = {
  subscription: ref(dummySubscription),
  subscriptionMetrics: ref(dummySubscriptionMetrics),
  subscriptionHealth: ref(dummySubscriptionHealth),
  subscriptionUndeliveredMessages: ref(dummyUndeliveredMessages),
  subscriptionLastUndeliveredMessage: ref(dummyUndeliveredMessage),
  error: ref(false),
  loading: computed(() => false),
};

describe('SubscriptionView', () => {
  beforeEach(async () => {
    await router.push(
      '/groups/pl.allegro.public.group' +
        '/topics/pl.allegro.public.group.DummyEvent' +
        '/subscriptions/foobar-service',
    );
  });

  it('should render data boxes if subscription data was successfully fetched', () => {
    // given
    mockedUseSubscription.mockReturnValueOnce(useSubscriptionStub);

    // when
    const { getByText } = render(SubscriptionView);

    // then
    expect(getByText('Subscription metrics')).toBeInTheDocument();
    expect(getByText('Service response metrics')).toBeInTheDocument();
    expect(getByText('Manage subscription messages')).toBeInTheDocument();
    expect(getByText('Properties')).toBeInTheDocument();
    expect(getByText('Last undelivered message')).toBeInTheDocument();
    expect(getByText('Subscription message filters')).toBeInTheDocument();
    expect(getByText('Fixed HTTP headers')).toBeInTheDocument();
    expect(getByText('Last 100 undelivered messages')).toBeInTheDocument();
  });

  it('should render subscription health alert', () => {
    // given
    mockedUseSubscription.mockReturnValueOnce(useSubscriptionStub);

    // when
    const { getByText } = render(SubscriptionView);

    // then
    expect(getByText(/subscription lagging/i)).toBeInTheDocument();
    expect(getByText(/subscription lag is growing/i)).toBeInTheDocument();
  });

  it('should show loading spinner when fetching subscription data', () => {
    // given
    mockedUseSubscription.mockReturnValueOnce({
      ...useSubscriptionStub,
      loading: computed(() => true),
    });

    // when
    const { queryByTestId } = render(SubscriptionView);

    // then
    expect(mockedUseSubscription).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('should hide loading spinner when data fetch is complete', () => {
    // given
    mockedUseSubscription.mockReturnValueOnce({
      ...useSubscriptionStub,
      loading: computed(() => false),
    });

    // when
    const { queryByTestId } = render(SubscriptionView);

    // then
    expect(mockedUseSubscription).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should show error message when fetching data failed', () => {
    // given
    mockedUseSubscription.mockReturnValueOnce({
      ...useSubscriptionStub,
      loading: computed(() => false),
      error: ref(true),
    });

    // when
    const { queryByText } = render(SubscriptionView);

    // then
    expect(mockedUseSubscription).toHaveBeenCalledOnce();
    expect(queryByText(/connection error/i)).toBeInTheDocument();
    expect(
      queryByText(/could not fetch foobar-service subscription details/i),
    ).toBeInTheDocument();
  });

  it('should not show error message when data was fetch successfully', () => {
    // given
    mockedUseSubscription.mockReturnValueOnce({
      ...useSubscriptionStub,
      loading: computed(() => false),
      error: ref(false),
    });

    // when
    const { queryByText } = render(SubscriptionView);

    // then
    expect(mockedUseSubscription).toHaveBeenCalledOnce();
    expect(queryByText(/connection error/i)).not.toBeInTheDocument();
  });
});
