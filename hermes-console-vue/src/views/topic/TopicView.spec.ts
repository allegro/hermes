import { beforeEach, describe, expect } from 'vitest';
import { computed, ref } from 'vue';
import { dummySubscription } from '@/dummy/subscription';
import {
  dummyTopic,
  dummyTopicMessagesPreview,
  dummyTopicMetrics,
  dummyTopicOwner,
} from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import { useSubscriptionsList } from '@/composables/subscription/use-subscriptions-list/useSubscriptionsList';
import { useTopic } from '@/composables/topic/use-topic/useTopic';
import { useTopicMessagesPreview } from '@/composables/topic/use-topic-messages-preview/useTopicMessagesPreview';
import { useTopicMetrics } from '@/composables/topic/use-topic-metric/useTopicMetrics';
import router from '@/router';
import TopicView from '@/views/topic/TopicView.vue';

vi.mock('@/composables/topic/use-topic/useTopic');
vi.mock(
  '@/composables/subscription/use-subscriptions-list/useSubscriptionsList',
);
vi.mock('@/composables/topic/use-topic-metric/useTopicMetrics');
vi.mock(
  '@/composables/topic/use-topic-messages-preview/useTopicMessagesPreview',
);

const useTopicMock: ReturnType<typeof useTopic> = {
  topic: ref(dummyTopic),
  owner: ref(dummyTopicOwner),
  topicError: ref(false),
  ownerError: ref(false),
  topicIsLoading: computed(() => false),
  ownerIsLoading: computed(() => false),
};

const useSubscriptionsListMock: ReturnType<typeof useSubscriptionsList> = {
  subscriptions: ref([dummySubscription]),
  error: ref(false),
  isLoading: computed(() => false),
};

const useTopicMetricsMock: ReturnType<typeof useTopicMetrics> = {
  data: ref(dummyTopicMetrics),
  error: ref(false),
  isLoading: computed(() => false),
};

const useTopicMessagesPreviewMock: ReturnType<typeof useTopicMessagesPreview> =
  {
    data: ref(dummyTopicMessagesPreview),
    error: ref(false),
    isLoading: computed(() => false),
  };

describe('TopicView', () => {
  beforeEach(async () => {
    await router.push(
      `/groups/pl.allegro.public.group` + `/topics/${dummyTopic.name}`,
    );
  });

  it('should render all view boxes', () => {
    // given
    mockApi({});
    const expectedTitles = [
      'topicView.header.topic',
      'topicView.metrics.title',
      'topicView.properties.title',
      'topicView.messagesPreview.title',
      'topicView.schema.title',
      'topicView.subscriptions.title (1)',
    ];

    // when
    const { getByText } = render(TopicView);

    // then
    expectedTitles.forEach((title) => {
      expect(getByText(title)).toBeVisible();
    });
  });

  it('should render error message when failed fetching topic data', () => {
    // given
    mockApi({
      topic: { ...useTopicMock, topic: ref(undefined), topicError: ref(true) },
    });

    // when
    const { getByText } = render(TopicView);

    // then
    expect(getByText('topicView.errorMessage.topicFetchFailed')).toBeVisible();
  });

  it('should not render error message when topic data was fetched successfully', () => {
    // given
    mockApi({});

    // when
    const { queryByText } = render(TopicView);

    // then
    expect(
      queryByText('topicView.errorMessage.topicFetchFailed'),
    ).not.toBeInTheDocument();
  });

  it('should render spinner while loading topic data', () => {
    // given
    mockApi({
      topic: {
        ...useTopicMock,
        topic: ref(undefined),
        owner: ref(undefined),
        topicIsLoading: computed(() => true),
        ownerIsLoading: computed(() => true),
      },
    });

    // when
    const { queryByTestId } = render(TopicView);

    // then
    expect(queryByTestId('loading-spinner')).toBeVisible();
  });

  it('should hide spinner when topic data is fetched', () => {
    // given
    mockApi({});

    // when
    const { queryByTestId } = render(TopicView);

    // then
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });
});

function mockApi({
  topic = useTopicMock,
  subscriptionsList = useSubscriptionsListMock,
  topicMetrics = useTopicMetricsMock,
  topicMessagesPreview = useTopicMessagesPreviewMock,
}) {
  vi.mocked(useTopic).mockReturnValueOnce(topic);
  vi.mocked(useSubscriptionsList).mockReturnValueOnce(subscriptionsList);
  vi.mocked(useTopicMetrics).mockReturnValueOnce(topicMetrics);
  vi.mocked(useTopicMessagesPreview).mockReturnValueOnce(topicMessagesPreview);
}
