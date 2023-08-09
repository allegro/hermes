import { beforeEach, describe, expect } from 'vitest';
import {
  dummySubscription,
  secondDummySubscription,
} from '@/dummy/subscription';
import {
  dummyOfflineClientsSource,
  dummyTopic,
  dummyTopicMessagesPreview,
  dummyTopicMetrics,
  dummyTopicOwner,
} from '@/dummy/topic';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import type { UseTopic } from '@/composables/topic/use-topic/useTopic';
import { useTopic } from '@/composables/topic/use-topic/useTopic';
import router from '@/router';
import TopicView from '@/views/topic/TopicView.vue';
import { createTestingPinia } from '@pinia/testing';
import { createTestingPiniaWithState } from '@/dummy/store';
import { dummyAppConfig } from '@/dummy/app-config';

vi.mock('@/composables/topic/use-topic/useTopic');

const useTopicMock: UseTopic = {
  topic: ref(dummyTopic),
  owner: ref(dummyTopicOwner),
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
  }),
  fetchTopic: () => Promise.resolve(),
  fetchOfflineClientsSource: () => Promise.resolve(),
};

describe('TopicView', () => {
  beforeEach(async () => {
    await router.push(
      `/groups/pl.allegro.public.group` + `/topics/${dummyTopic.name}`,
    );
  });

  it('should call useTopic composable with correct topic name on render', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);

    // when
    render(TopicView, { testPinia: createTestingPiniaWithState() });

    // then
    expect(useTopic).toHaveBeenCalledOnce();
    expect(useTopic).toHaveBeenCalledWith(dummyTopic.name);
  });

  it('should fetch topic on render', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);
    const fetchTopicSpy = vi.spyOn(useTopicMock, 'fetchTopic');

    // when
    render(TopicView, { testPinia: createTestingPiniaWithState() });

    // then
    expect(fetchTopicSpy).toHaveBeenCalledOnce();
  });

  it('should render all view boxes', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce({
      ...useTopicMock,
      offlineClientsSource: ref(dummyOfflineClientsSource),
    });
    const expectedTitles = [
      'topicView.header.topic',
      'topicView.metrics.title',
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

  it('should not render offline clients when they are disabled in app config', () => {
    // given
    vi.mocked(useTopic).mockReturnValueOnce(useTopicMock);

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
      }),
    });

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

    // when
    const { queryByTestId } = render(TopicView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });
});
