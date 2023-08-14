import { computed, ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useStats } from '@/composables/use-stats/useStats';
import StatsView from '@/views/stats/StatsView.vue';

vi.mock('@/composables/use-stats/useStats');

const useStatsStub: ReturnType<typeof useStats> = {
  error: ref(false),
  loading: computed(() => false),
  topicCount: ref(100),
  ackAllTopicCount: ref(50),
  ackAllTopicShare: ref(0.5),
  trackingEnabledTopicCount: ref(20),
  trackingEnabledTopicShare: ref(0.2),
  avroTopicCount: ref(10),
  avroTopicShare: ref(0.1),
  subscriptionCount: ref(1000),
  trackingEnabledSubscriptionCount: ref(100),
  trackingEnabledSubscriptionShare: ref(0.1),
  avroSubscriptionCount: ref(500),
  avroSubscriptionShare: ref(0.5),
};

describe('StatsView', () => {
  it('should render if stats data was successfully fetched', () => {
    // given
    vi.mocked(useStats).mockReturnValueOnce(useStatsStub);

    // when
    const { getByText } = render(StatsView);

    // then
    expect(vi.mocked(useStats)).toHaveBeenCalledOnce();
    expect(getByText('stats.topics')).toBeVisible();
  });

  it('should show loading spinner when fetching stats data', () => {
    // given
    vi.mocked(useStats).mockReturnValueOnce({
      ...useStatsStub,
      loading: computed(() => true),
    });

    // when
    const { queryByTestId } = render(StatsView);

    // then
    expect(vi.mocked(useStats)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeVisible();
  });

  it('should hide loading spinner when data fetch is complete', () => {
    // given
    vi.mocked(useStats).mockReturnValueOnce({
      ...useStatsStub,
      loading: computed(() => false),
    });

    // when
    const { queryByTestId } = render(StatsView);

    // then
    expect(vi.mocked(useStats)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should show error message when fetching data failed', () => {
    // given
    vi.mocked(useStats).mockReturnValueOnce({
      ...useStatsStub,
      loading: computed(() => false),
      error: ref(true),
    });

    // when
    const { queryByText } = render(StatsView);

    // then
    expect(vi.mocked(useStats)).toHaveBeenCalledOnce();
    expect(queryByText('stats.connectionError.title')).toBeVisible();
    expect(queryByText('stats.connectionError.text')).toBeVisible();
  });

  it('should not show error message when data was fetch successfully', () => {
    // given
    vi.mocked(useStats).mockReturnValueOnce({
      ...useStatsStub,
      loading: computed(() => false),
      error: ref(false),
    });

    // when
    const { queryByText } = render(StatsView);

    // then
    expect(vi.mocked(useStats)).toHaveBeenCalledOnce();
    expect(queryByText('stats.connectionError.title')).not.toBeInTheDocument();
  });
});
