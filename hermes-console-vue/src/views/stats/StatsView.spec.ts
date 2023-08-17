import { computed, ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useStats } from '@/composables/use-stats/useStats';
import StatsView from '@/views/stats/StatsView.vue';

vi.mock('@/composables/use-stats/useStats');

const useStatsStub: ReturnType<typeof useStats> = {
  error: ref({ fetchError: null }),
  loading: computed(() => false),
  stats: ref({
    topicCount: 100,
    ackAllTopicCount: 50,
    ackAllTopicShare: 0.5,
    trackingEnabledTopicCount: 20,
    trackingEnabledTopicShare: 0.2,
    avroTopicCount: 10,
    avroTopicShare: 0.1,
    subscriptionCount: 1000,
    trackingEnabledSubscriptionCount: 100,
    trackingEnabledSubscriptionShare: 0.1,
    avroSubscriptionCount: 500,
    avroSubscriptionShare: 0.5,
  }),
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
      error: ref({ fetchError: new Error() }),
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
    });

    // when
    const { queryByText } = render(StatsView);

    // then
    expect(vi.mocked(useStats)).toHaveBeenCalledOnce();
    expect(queryByText('stats.connectionError.title')).not.toBeInTheDocument();
  });
});
