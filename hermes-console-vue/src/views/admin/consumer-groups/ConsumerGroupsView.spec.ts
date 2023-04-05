import { beforeEach } from 'vitest';
import { computed, ref } from 'vue';
import { dummyConsumerGroups } from '@/dummy/consumerGroups';
import { render } from '@/utils/test-utils';
import { useConsumerGroups } from '@/composables/use-consumer-groups/useConsumerGroups';
import ConsumerGroupsView from '@/views/admin/consumer-groups/ConsumerGroupsView.vue';
import router from '@/router';

vi.mock('@/composables/use-consumer-groups/useConsumerGroups');

const useConsumerGroupsStub: ReturnType<typeof useConsumerGroups> = {
  consumerGroups: ref(dummyConsumerGroups),
  loading: computed(() => false),
  error: ref(false),
};

describe('ConsumerGroupsView', () => {
  beforeEach(async () => {
    await router.push(
      '/groups/pl.allegro.public.group' +
        '/topics/pl.allegro.public.group.DummyEvent' +
        '/subscriptions/foobar-service',
    );
  });

  it('should render if consumerGroups data was successfully fetched', () => {
    // given
    vi.mocked(useConsumerGroups).mockReturnValueOnce(useConsumerGroupsStub);

    // when
    const { getByText } = render(ConsumerGroupsView);

    // then
    expect(vi.mocked(useConsumerGroups)).toHaveBeenCalledOnce();
    expect(getByText('consumerGroups.title')).toBeInTheDocument();
    expect(getByText('consumerGroups.groupId')).toBeInTheDocument();
  });

  it('should show loading spinner when fetching consumerGroups data', () => {
    // given
    vi.mocked(useConsumerGroups).mockReturnValueOnce({
      ...useConsumerGroupsStub,
      loading: computed(() => true),
    });

    // when
    const { queryByTestId } = render(ConsumerGroupsView);

    // then
    expect(vi.mocked(useConsumerGroups)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('should hide loading spinner when data fetch is complete', () => {
    // given
    vi.mocked(useConsumerGroups).mockReturnValueOnce({
      ...useConsumerGroupsStub,
      loading: computed(() => false),
    });

    // when
    const { queryByTestId } = render(ConsumerGroupsView);

    // then
    expect(vi.mocked(useConsumerGroups)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should show error message when fetching data failed', () => {
    // given
    vi.mocked(useConsumerGroups).mockReturnValueOnce({
      ...useConsumerGroupsStub,
      loading: computed(() => false),
      error: ref(true),
    });

    // when
    const { queryByText } = render(ConsumerGroupsView);

    // then
    expect(vi.mocked(useConsumerGroups)).toHaveBeenCalledOnce();
    expect(
      queryByText('consumerGroups.connectionError.title'),
    ).toBeInTheDocument();
    expect(
      queryByText('consumerGroups.connectionError.text'),
    ).toBeInTheDocument();
  });

  it('should not show error message when data was fetch successfully', () => {
    // given
    vi.mocked(useConsumerGroups).mockReturnValueOnce({
      ...useConsumerGroupsStub,
      loading: computed(() => false),
      error: ref(false),
    });

    // when
    const { queryByText } = render(ConsumerGroupsView);

    // then
    expect(vi.mocked(useConsumerGroups)).toHaveBeenCalledOnce();
    expect(
      queryByText('consumerGroups.connectionError.title'),
    ).not.toBeInTheDocument();
  });
});
