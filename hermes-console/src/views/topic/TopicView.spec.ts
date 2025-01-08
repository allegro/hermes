import { beforeEach, describe, expect } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { createTestingPinia } from '@pinia/testing';
import { createTestingPiniaWithState } from '@/dummy/store';
import { dummyAppConfig } from '@/dummy/app-config';
import { dummyMetricsDashboardUrl } from '@/dummy/metricsDashboardUrl';
import {
  dummyOfflineClientsSource,
  dummyOwner,
  dummyTopic,
  dummyTopicMessagesPreview,
  dummyTopicMetrics,
} from '@/dummy/topic';
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
import type { UseMetrics } from '@/composables/metrics/use-metrics/useMetrics';
import type { UseRoles } from '@/composables/roles/use-roles/useRoles';
import type { UseTopic } from '@/composables/topic/use-topic/useTopic';

vi.mock('@/composables/topic/use-topic/useTopic');
vi.mock('@/composables/roles/use-roles/useRoles');

const useRolesStub: UseRoles = {
  roles: ref(dummyRoles),
  loading: ref(false),
  error: ref({
    fetchRoles: null,
  }),
};

const useTopicMock: UseTopic = {
  topic: ref(dummyTopic),
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
};

vi.mock('@/composables/metrics/use-metrics/useMetrics');

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
    render(TopicView, { testPinia: createTestingPiniaWithState() });

    // then
    expect(useTopic).toHaveBeenCalledOnce();
    expect(useTopic).toHaveBeenCalledWith(dummyTopic.name);
  });

  it('should render all view boxes', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce({
      ...useTopicMock,
      offlineClientsSource: ref(dummyOfflineClientsSource),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    const expectedTitles = [
      'topicView.header.topic',
      'topicView.metrics.title',
      'costsCard.title',
      'topicView.properties.title',
      'topicView.messagesPreview.title',
      'topicView.schema.title',
      'topicView.subscriptions.title (2)',
      'topicView.offlineClients.title',
    ];

    // when
    const { getByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expectedTitles.forEach((title) => {
      expect(getByText(title)).toBeVisible();
    });
  });

  it('should not render messages preview when they are disabled in app config', () => {
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

    // then
    expect(
      queryByText('topicView.messagesPreview.title'),
    ).not.toBeInTheDocument();
  });

  it('should not render messages preview when unauthorized', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    vi.mocked(useRoles).mockReturnValueOnce({
      ...useRolesStub,
      roles: ref([]),
    });

    // when
    const { queryByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(
      queryByText('topicView.messagesPreview.title'),
    ).not.toBeInTheDocument();
  });

  it('should not render offline clients when they are disabled in app config', () => {
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
      queryByText('topicView.offlineClients.title'),
    ).not.toBeInTheDocument();
  });

  it('should not render costs card when it is disabled in app config', () => {
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

  it('should not render offline clients when topic has disabled offline storage', () => {
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
      queryByText('topicView.offlineClients.title'),
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

  it('should render tracking card when tracking is enabled', () => {
    // given
    const dummyTopic2 = dummyTopic;
    dummyTopic2.trackingEnabled = true;

    // and
    vi.mocked(useTopic).mockReturnValueOnce({
      ...useTopicMock,
      topic: ref(dummyTopic2),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { getByText } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(getByText('trackingCard.title')).toBeVisible();
  });
});
