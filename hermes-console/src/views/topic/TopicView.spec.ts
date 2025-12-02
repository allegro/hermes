import { beforeEach, describe, expect } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { createTestingPinia } from '@pinia/testing';
import { createTestingPiniaWithState } from '@/dummy/store';
import {
  dummyActiveOfflineRetransmissions,
  dummyOfflineClientsSource,
  dummyOwner,
  dummyTopic,
  dummyTopicMessagesPreview,
  dummyTopicMetrics,
} from '@/dummy/topic';
import { dummyAppConfig } from '@/dummy/app-config';
import { dummyMetricsDashboardUrl } from '@/dummy/metricsDashboardUrl';
import { dummyRoles } from '@/dummy/roles';
import {
  dummySubscription,
  secondDummySubscription,
} from '@/dummy/subscription';
import { dummyTrackingUrls } from '@/dummy/tracking-urls';
import { fireEvent } from '@testing-library/vue';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useMetrics } from '@/composables/metrics/use-metrics/useMetrics';
import { useRoles } from '@/composables/roles/use-roles/useRoles';
import { useTopic } from '@/composables/topic/use-topic/useTopic';
import router from '@/router';
import TopicView from '@/views/topic/TopicView.vue';
import userEvent from '@testing-library/user-event';
import type { UseMetrics } from '@/composables/metrics/use-metrics/useMetrics';
import type { UseRoles } from '@/composables/roles/use-roles/useRoles';
import type { UseTopic } from '@/composables/topic/use-topic/useTopic';

vi.mock('@/composables/topic/use-topic/useTopic');
vi.mock('@/composables/roles/use-roles/useRoles');
vi.mock('@/composables/metrics/use-metrics/useMetrics');

const useRolesStub: UseRoles = {
  roles: ref(dummyRoles),
  loading: ref(false),
  error: ref({
    fetchRoles: null,
  }),
};

const useTopicMock: UseTopic = {
  topic: ref({ ...dummyTopic, trackingEnabled: true }),
  owner: ref(dummyOwner),
  messages: ref(dummyTopicMessagesPreview),
  metrics: ref(dummyTopicMetrics),
  subscriptions: ref([dummySubscription, secondDummySubscription]),
  offlineClientsSource: ref(undefined),
  loading: ref(false),
  error: ref({
    fetchTopic: null,
    fetchOwner: null,
    fetchTopicMessagesPreview: null,
    fetchTopicMetrics: null,
    fetchSubscriptions: null,
    fetchOfflineClientsSource: null,
    getTopicTrackingUrls: null,
  }),
  trackingUrls: ref(dummyTrackingUrls),
  fetchOfflineClientsSource: () => Promise.resolve(),
  removeTopic: () => Promise.resolve(true),
  fetchTopicClients: () => Promise.resolve([dummySubscription.name]),
  activeRetransmissions: ref(dummyActiveOfflineRetransmissions),
};

const useMetricsStub: UseMetrics = {
  dashboardUrl: ref(dummyMetricsDashboardUrl.url),
  loading: ref(false),
  error: ref({
    fetchDashboardUrl: null,
  }),
};

describe('TopicView', () => {
  beforeEach(async () => {
    setActivePinia(createPinia());
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    await router.push(
      `/ui/groups/pl.allegro.public.group` + `/topics/${dummyTopic.name}`,
    );
  });

  it('should call useTopic composable with correct topic name on render', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(vi.mocked(useTopic)).toHaveBeenCalledOnce();
    // TODO: Fix assertion after adding reactivity to route params
    // expect(vi.mocked(useTopic)).toHaveBeenCalledWith(expectedTopicName);
  });

  it('should render all tabs', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce({
      ...useTopicMock,
      offlineClientsSource: ref(dummyOfflineClientsSource),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    const expectedTabs = [
      'topicView.tabs.general',
      'topicView.tabs.schema',
      'topicView.tabs.subscriptions',
      'topicView.tabs.offlineClients',
      'topicView.tabs.messages',
      'topicView.tabs.offlineRetransmission',
    ];

    // when
    const { getByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expectedTabs.forEach((title) => {
      expect(getByText(title)).toBeVisible();
    });
  });

  it.each([
    'topicView.tabs.general',
    'topicView.tabs.schema',
    'topicView.tabs.subscriptions',
    'topicView.tabs.offlineClients',
    'topicView.tabs.messages',
    'topicView.tabs.offlineRetransmission',
  ])('should activate tab on click', async (tab: string) => {
    // given
    const user = userEvent.setup();
    vi.mocked(useTopic).mockReturnValueOnce({
      ...useTopicMock,
      offlineClientsSource: ref(dummyOfflineClientsSource),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    const { getByText } = render(TopicView, {
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
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    const { getByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await user.click(getByText('topicView.tabs.general'));

    // then
    expect(getByText('topicView.metrics.title')).toBeVisible();
    expect(getByText('topicView.properties.title')).toBeVisible();
    expect(getByText('costsCard.title')).toBeVisible();
  });

  it('should show schema on schema tab click', async () => {
    // given
    const user = userEvent.setup();
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    const { getByText, getByTestId } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await user.click(getByText('topicView.tabs.schema'));

    // then
    expect(getByTestId('avro-viewer')).toBeVisible();
  });

  it('should show list of subscriptions on subscriptions tab click', async () => {
    // given
    const user = userEvent.setup();
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    const { getByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await user.click(getByText('topicView.tabs.subscriptions'));

    // then
    expect(getByText(dummySubscription.name)).toBeVisible();
  });

  it('should show offline clients on offline clients tab click', async () => {
    // given
    const user = userEvent.setup();
    vi.mocked(useTopic).mockReturnValueOnce({
      ...useTopicMock,
      offlineClientsSource: ref(dummyOfflineClientsSource),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    const { getByText, getByTestId } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await user.click(getByText('topicView.tabs.offlineClients'));

    // then
    expect(getByTestId('offline-clients')).toBeVisible();
  });

  it('should show appropriate sections on messages tab click', async () => {
    // given
    const user = userEvent.setup();
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    const { getByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await user.click(getByText('topicView.tabs.messages'));

    // then
    expect(getByText('topicView.messagesPreview.title')).toBeVisible();
    expect(getByText('trackingCard.title')).toBeVisible();
  });

  it('should show appropriate sections on offline retransmission tab click', async () => {
    // given
    const user = userEvent.setup();
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    const { getByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // when
    await user.click(getByText('topicView.tabs.offlineRetransmission'));

    // then
    expect(
      getByText('offlineRetransmission.monitoringView.title'),
    ).toBeVisible();
  });

  it('should not display messages preview when they are disabled in app config', async () => {
    // given
    const user = userEvent.setup();
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { getByText } = render(TopicView, {
      testPinia: createTestingPinia({
        initialState: {
          appConfig: {
            appConfig: {
              ...dummyAppConfig,
              topic: { ...dummyAppConfig.topic, messagePreviewEnabled: false },
            },
            loading: false,
            error: {
              loadConfig: null,
            },
          },
        },
      }),
    });

    await user.click(getByText('topicView.tabs.messages'));

    // then
    expect(
      getByText('topicView.messagesPreview.messageDetails.disabled'),
    ).toBeVisible();
  });

  it('should not display messages preview when unauthorized', async () => {
    // given
    const user = userEvent.setup();
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce({
      ...useRolesStub,
      roles: ref([]),
    });

    // when
    const { getByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    await user.click(getByText('topicView.tabs.messages'));

    // then
    expect(
      getByText('topicView.messagesPreview.messageDetails.disabled'),
    ).toBeVisible();
  });

  it('should not show offline clients tab when they are disabled in app config', async () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByText } = render(TopicView, {
      testPinia: createTestingPinia({
        initialState: {
          appConfig: {
            appConfig: {
              ...dummyAppConfig,
              topic: { ...dummyAppConfig.topic, offlineClientsEnabled: false },
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
    expect(
      queryByText('topicView.tabs.offlineClients'),
    ).not.toBeInTheDocument();
  });

  it('should not render costs card when it is disabled in app config', async () => {
    // given
    const user = userEvent.setup();
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { getByText, queryByText } = render(TopicView, {
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

    await user.click(getByText('topicView.tabs.general'));

    // then
    expect(queryByText('costsCard.title')).not.toBeInTheDocument();
  });

  it('should not show offline clients tab when topic has disabled offline storage', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce({
      ...useTopicMock,
      topic: ref({
        ...dummyTopic,
        offlineStorage: {
          enabled: false,
          retentionTime: {
            duration: 60,
            infinite: false,
          },
        },
      }),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(
      queryByText('topicView.tabs.offlineClients'),
    ).not.toBeInTheDocument();
  });

  it('should render error message when failed fetching topic data', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce({
      ...useTopicMock,
      topic: ref(undefined),
      error: ref({
        fetchTopic: new Error('Sample error'),
        fetchOwner: null,
        fetchTopicMessagesPreview: null,
        fetchTopicMetrics: null,
        fetchSubscriptions: null,
        fetchOfflineClientsSource: null,
        getTopicTrackingUrls: null,
      }),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { getByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(getByText('topicView.errorMessage.topicFetchFailed')).toBeVisible();
  });

  it('should not render error message when topic data was fetched successfully', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(
      queryByText('topicView.errorMessage.topicFetchFailed'),
    ).not.toBeInTheDocument();
  });

  it('should render spinner while loading is indicated', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce({
      ...useTopicMock,
      loading: ref(true),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByTestId } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(queryByTestId('loading-spinner')).toBeVisible();
  });

  it('should hide spinner when topic data is fetched', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByTestId } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should show confirmation dialog on remove button click', async () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { getByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });
    await fireEvent.click(getByText('topicView.header.actions.remove'));

    // then
    expect(
      getByText('topicView.confirmationDialog.remove.title'),
    ).toBeInTheDocument();
    expect(
      getByText('topicView.confirmationDialog.remove.text'),
    ).toBeInTheDocument();
  });
});
