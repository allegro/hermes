import { beforeEach } from 'vitest';
import { dummyConsumerGroups } from '@/dummy/consumerGroups';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useConsumerGroups } from '@/composables/consumer-groups/use-consumer-groups/useConsumerGroups';
import ConsumerGroupsView from '@/views/admin/consumer-groups/ConsumerGroupsView.vue';
import router from '@/router';
import type { UseConsumerGroups } from '@/composables/consumer-groups/use-consumer-groups/useConsumerGroups';

vi.mock('@/composables/consumer-groups/use-consumer-groups/useConsumerGroups');

const useConsumerGroupsStub: UseConsumerGroups = {
  consumerGroups: ref(dummyConsumerGroups),
  loading: ref(false),
  error: ref({
    fetchConsumerGroups: null,
  }),
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
    expect(getByText('consumerGroups.title')).toBeVisible();
    expect(getByText('consumerGroups.groupId')).toBeVisible();
  });

  it('should show loading spinner when fetching consumerGroups data', () => {
    // given
    vi.mocked(useConsumerGroups).mockReturnValueOnce({
      ...useConsumerGroupsStub,
      loading: ref(true),
    });

    // when
    const { queryByTestId } = render(ConsumerGroupsView);

    // then
    expect(vi.mocked(useConsumerGroups)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeVisible();
  });

  it('should hide loading spinner when data fetch is complete', () => {
    // given
    vi.mocked(useConsumerGroups).mockReturnValueOnce({
      ...useConsumerGroupsStub,
      loading: ref(false),
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
      loading: ref(false),
      error: ref({ fetchConsumerGroups: new Error() }),
    });

    // when
    const { queryByText } = render(ConsumerGroupsView);

    // then
    expect(vi.mocked(useConsumerGroups)).toHaveBeenCalledOnce();
    expect(queryByText('consumerGroups.connectionError.title')).toBeVisible();
    expect(queryByText('consumerGroups.connectionError.text')).toBeVisible();
  });

  it('should not show error message when data was fetch successfully', () => {
    // given
    vi.mocked(useConsumerGroups).mockReturnValueOnce({
      ...useConsumerGroupsStub,
      loading: ref(false),
      error: ref({ fetchConsumerGroups: null }),
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
