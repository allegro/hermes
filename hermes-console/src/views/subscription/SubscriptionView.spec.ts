import { beforeEach, expect } from 'vitest';
import { computed, ref } from 'vue';
import { createPinia, setActivePinia } from 'pinia';
import { createTestingPinia } from '@pinia/testing';
import { createTestingPiniaWithState } from '@/dummy/store';
import { dummyAppConfig } from '@/dummy/app-config';
import { dummyMetricsDashboardUrl } from '@/dummy/metricsDashboardUrl';
import { dummyOwner } from '@/dummy/topic';
import {
  dummySubscription,
  dummySubscriptionHealth,
  dummySubscriptionMetrics,
  dummyUndeliveredMessage,
  dummyUndeliveredMessages,
} from '@/dummy/subscription';
import { dummyTrackingUrls } from '@/dummy/tracking-urls';
import { fireEvent } from '@testing-library/vue';
import { render } from '@/utils/test-utils';
import { Role } from '@/api/role';
import { State, type Subscription } from '@/api/subscription';
import { useMetrics } from '@/composables/metrics/use-metrics/useMetrics';
import { useRoles } from '@/composables/roles/use-roles/useRoles';
import { useSubscription } from '@/composables/subscription/use-subscription/useSubscription';
import router from '@/router';
import SubscriptionView from '@/views/subscription/SubscriptionView.vue';
import TopicView from '@/views/topic/TopicView.vue';
import userEvent from '@testing-library/user-event';
import type { UseMetrics } from '@/composables/metrics/use-metrics/useMetrics';
import type { UseRoles } from '@/composables/roles/use-roles/useRoles';

vi.mock('@/composables/subscription/use-subscription/useSubscription');

const useSubscriptionStub: ReturnType<typeof useSubscription> = {
  subscription: ref(dummySubscription),
  owner: ref(dummyOwner),
  subscriptionMetrics: ref(dummySubscriptionMetrics),
  subscriptionHealth: ref(dummySubscriptionHealth),
  subscriptionUndeliveredMessages: ref(dummyUndeliveredMessages),
  subscriptionLastUndeliveredMessage: ref(dummyUndeliveredMessage),
  trackingUrls: ref(dummyTrackingUrls),
  retransmitting: computed(() => false),
  skippingAllMessages: computed(() => false),
  error: ref({
    fetchSubscription: null,
    fetchOwner: null,
    fetchSubscriptionMetrics: null,
    fetchSubscriptionHealth: null,
    fetchSubscriptionUndeliveredMessages: null,
    fetchSubscriptionLastUndeliveredMessage: null,
    getSubscriptionTrackingUrls: null,
  }),
  loading: computed(() => false),
  removeSubscription: () => Promise.resolve(true),
  suspendSubscription: () => Promise.resolve(true),
  activateSubscription: () => Promise.resolve(true),
  retransmitMessages: () => Promise.resolve(true),
  skipAllMessages: () => Promise.resolve(true),
};

vi.mock('@/composables/roles/use-roles/useRoles');

const useRolesStub: UseRoles = {
  roles: ref([Role.SUBSCRIPTION_OWNER]),
  loading: ref(false),
  error: ref({
    fetchRoles: null,
  }),
};

vi.mock('@/composables/metrics/use-metrics/useMetrics');

const useMetricsStub: UseMetrics = {
  dashboardUrl: ref(dummyMetricsDashboardUrl.url),
  loading: ref(false),
  error: ref({
    fetchDashboardUrl: null,
  }),
};

describe('SubscriptionView', () => {
  beforeEach(async () => {
    setActivePinia(createPinia());
    await router.push(
      '/ui/groups/pl.allegro.public.group' +
        '/topics/pl.allegro.public.group.DummyEvent' +
        '/subscriptions/foobar-service',
    );
  });

  it('should render all tabs if subscription data was successfully fetched', () => {
    // given
    vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);

    // when
    const { getByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    const expectedTabs = [
      'subscription.tabs.general',
      'subscription.tabs.filters',
      'subscription.tabs.messages',
    ];
    expectedTabs.forEach((boxTitle) => {
      expect(getByText(boxTitle)).toBeVisible();
    });
  });

  it.each([
    'subscription.tabs.general',
    'subscription.tabs.filters',
    'subscription.tabs.messages',
  ])('should activate tab on click', async (tab: string) => {
    // given
    const user = userEvent.setup();
    vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    const { getByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    const tabElement = getByText(tab).closest('button')!;
    await user.click(tabElement);

    // then
    expect(tabElement).toHaveClass('v-tab--selected');
  });

  it('should show appropriate sections on general tab click', async () => {
    // given
    const user = userEvent.setup();
    vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    const { getByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await user.click(getByText('subscription.tabs.general'));

    // then
    expect(getByText('subscription.metricsCard.title')).toBeVisible();
    expect(getByText('costsCard.title')).toBeVisible();
    expect(getByText('subscription.propertiesCard.title')).toBeVisible();
  });

  it('should show appropriate sections on filters tab click', async () => {
    // given
    const user = userEvent.setup();
    vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    const { getByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await user.click(getByText('subscription.tabs.filters'));

    // then
    expect(getByText('subscription.filtersCard.title')).toBeVisible();
  });

  it('should show appropriate sections on mutations tab click', async () => {
    // given
    const user = userEvent.setup();
    vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    const { getByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await user.click(getByText('subscription.tabs.mutations'));

    // then
    expect(getByText('subscription.headersCard.title')).toBeVisible();
  });

  it('should show appropriate sections on messages tab click', async () => {
    // given
    const user = userEvent.setup();
    vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    const { getByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await user.click(getByText('subscription.tabs.messages'));

    // then
    expect(getByText('subscription.manageMessagesCard.title')).toBeVisible();
    expect(
      getByText('subscription.undeliveredMessagesCard.title'),
    ).toBeVisible();
    expect(
      getByText('subscription.lastUndeliveredMessage.title'),
    ).toBeVisible();
    expect(getByText('trackingCard.title')).toBeVisible();
  });

  it.each([
    {
      tab: 'subscription.tabs.messages',
      box: 'subscription.manageMessagesCard.title',
    },
    {
      tab: 'subscription.tabs.messages',
      box: 'subscription.undeliveredMessagesCard.title',
    },
    {
      tab: 'subscription.tabs.messages',
      box: 'subscription.lastUndeliveredMessage.title',
    },
  ])(
    'should not render some boxes if user is unauthorized',
    async ({ tab, box }: { tab: string; box: string }) => {
      // given
      const user = userEvent.setup();
      vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);
      vi.mocked(useRoles).mockReturnValueOnce({
        ...useRolesStub,
        roles: ref([]),
      });
      vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);

      // when
      const { queryByText, getByText } = render(SubscriptionView, {
        testPinia: createTestingPiniaWithState(),
      });
      await user.click(getByText(tab));

      // then
      expect(queryByText(box)).not.toBeInTheDocument();
    },
  );

  it('should render subscription health alert', () => {
    // given
    vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);

    // when
    const { getByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });

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
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByTestId } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });

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
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByTestId } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(vi.mocked(useSubscription)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should show error message when fetching data failed', () => {
    // given
    vi.mocked(useSubscription).mockReturnValueOnce({
      ...useSubscriptionStub,
      loading: computed(() => false),
      error: ref({
        fetchSubscription: new Error('Sample error'),
        fetchOwner: null,
        fetchSubscriptionMetrics: null,
        fetchSubscriptionHealth: null,
        fetchSubscriptionUndeliveredMessages: null,
        fetchSubscriptionLastUndeliveredMessage: null,
        getSubscriptionTrackingUrls: null,
      }),
    });
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });

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
    });
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(vi.mocked(useSubscription)).toHaveBeenCalledOnce();
    expect(
      queryByText('subscription.connectionError.title'),
    ).not.toBeInTheDocument();
  });

  it('should show confirmation dialog on remove button click', async () => {
    // given
    vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);

    // when
    const { getByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });
    await fireEvent.click(
      getByText('subscription.subscriptionMetadata.actions.remove'),
    );

    // then
    expect(
      getByText('subscription.confirmationDialog.remove.title'),
    ).toBeInTheDocument();
    expect(
      getByText('subscription.confirmationDialog.remove.text'),
    ).toBeInTheDocument();
  });

  it('should show confirmation dialog on suspend button click', async () => {
    // given
    vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);

    // when
    const { getByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });
    await fireEvent.click(
      getByText('subscription.subscriptionMetadata.actions.suspend'),
    );

    // then
    expect(
      getByText('subscription.confirmationDialog.suspend.title'),
    ).toBeInTheDocument();
    expect(
      getByText('subscription.confirmationDialog.suspend.text'),
    ).toBeInTheDocument();
  });

  it('should show confirmation dialog on activate button click', async () => {
    // given
    vi.mocked(useSubscription).mockReturnValueOnce({
      ...useSubscriptionStub,
      subscription: ref({
        ...dummySubscription,
        state: ref(State.SUSPENDED),
      }),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);

    // when
    const { getByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });
    await fireEvent.click(
      getByText('subscription.subscriptionMetadata.actions.activate'),
    );

    // then
    expect(
      getByText('subscription.confirmationDialog.activate.title'),
    ).toBeInTheDocument();
    expect(
      getByText('subscription.confirmationDialog.activate.text'),
    ).toBeInTheDocument();
  });

  it('should not render costs card when it is disabled in app config', () => {
    // given
    vi.mocked(useSubscription).mockReturnValueOnce(useSubscriptionStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);

    // when
    const { queryByText } = render(TopicView, {
      testPinia: createTestingPinia({
        initialState: {
          appConfig: {
            appConfig: {
              ...dummyAppConfig,
              costs: {
                enabled: false,
              },
            },
            loading: false,
            error: {
              loadConfig: null,
            },
          },
        },
      }),
    });

    // then
    expect(queryByText('costsCard.title')).not.toBeInTheDocument();
  });

  it('should not render tracking when it is disabled', async () => {
    // given
    const user = userEvent.setup();
    const dummySubscription2: Subscription = {
      ...dummySubscription,
      trackingEnabled: false,
    };

    // and
    vi.mocked(useSubscription).mockReset();
    vi.mocked(useSubscription).mockReturnValueOnce({
      ...useSubscriptionStub,
      subscription: ref(dummySubscription2),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);

    // when
    const { getByText, queryByText } = render(SubscriptionView, {
      testPinia: createTestingPiniaWithState(),
    });
    await user.click(getByText('subscription.tabs.messages'));

    // then
    expect(queryByText('trackingCard.title')).not.toBeInTheDocument();
  });
});
