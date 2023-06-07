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
import { useSubscription } from '@/composables/topic-subscriptions/use-subscription/useSubscription';
import router from '@/router';
import SubscriptionView from '@/views/subscription/SubscriptionView.vue';

vi.mock('@/composables/topic-subscriptions/use-subscription/useSubscription');

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
    vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);

    // when
    const { getByText } = render(SubscriptionView);

    // then
    const cardTitles = [
      'subscription.metricsCard.title',
      'subscription.serviceResponseMetrics.title',
      'subscription.manageMessagesCard.title',
      'subscription.propertiesCard.title',
      'subscription.lastUndeliveredMessage.title',
      'subscription.filtersCard.title',
      'subscription.headersCard.title',
      'subscription.undeliveredMessagesCard.title',
    ];
    cardTitles.forEach((boxTitle) => {
      expect(getByText(boxTitle)).toBeVisible();
    });
  });

  it('should render subscription health alert', () => {
    // given
    vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);

    // when
    const { getByText } = render(SubscriptionView);

    // then
    expect(
      getByText('subscription.healthProblemsAlerts.lagging.title'),
    ).toBeVisible();
    expect(
      getByText('subscription.healthProblemsAlerts.lagging.text'),
    ).toBeVisible();
  });

  it('should show loading spinner when fetching subscription data', () => {
    // given
    vi.mocked(useSubscription).mockReturnValueOnce({
      ...useSubscriptionStub,
      loading: computed(() => true),
    });

    // when
    const { queryByTestId } = render(SubscriptionView);

    // then
    expect(vi.mocked(useSubscription)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeVisible();
  });

  it('should hide loading spinner when data fetch is complete', () => {
    // given
    vi.mocked(useSubscription).mockReturnValueOnce({
      ...useSubscriptionStub,
      loading: computed(() => false),
    });

    // when
    const { queryByTestId } = render(SubscriptionView);

    // then
    expect(vi.mocked(useSubscription)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should show error message when fetching data failed', () => {
    // given
    vi.mocked(useSubscription).mockReturnValueOnce({
      ...useSubscriptionStub,
      loading: computed(() => false),
      error: ref(true),
    });

    // when
    const { queryByText } = render(SubscriptionView);

    // then
    expect(vi.mocked(useSubscription)).toHaveBeenCalledOnce();
    expect(queryByText('subscription.connectionError.title')).toBeVisible();
    expect(queryByText('subscription.connectionError.text')).toBeVisible();
  });

  it('should not show error message when data was fetch successfully', () => {
    // given
    vi.mocked(useSubscription).mockReturnValueOnce({
      ...useSubscriptionStub,
      loading: computed(() => false),
      error: ref(false),
    });

    // when
    const { queryByText } = render(SubscriptionView);

    // then
    expect(vi.mocked(useSubscription)).toHaveBeenCalledOnce();
    expect(
      queryByText('subscription.connectionError.title'),
    ).not.toBeInTheDocument();
  });
});
