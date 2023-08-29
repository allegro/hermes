import { computed, ref } from 'vue';
import { createTestingPiniaWithState } from '@/dummy/store';
import { dummyDatacentersReadiness } from '@/dummy/readiness';
import { expect } from 'vitest';
import { fireEvent } from '@testing-library/vue';
import { render } from '@/utils/test-utils';
import { useReadiness } from '@/composables/readiness/use-readiness/useReadiness';
import ReadinessView from '@/views/admin/readiness/ReadinessView.vue';
import type { UseReadiness } from '@/composables/readiness/use-readiness/useReadiness';

vi.mock('@/composables/readiness/use-readiness/useReadiness');

const useReadinessStub: UseReadiness = {
  datacentersReadiness: ref(dummyDatacentersReadiness),
  error: ref({
    fetchReadiness: null,
  }),
  loading: ref(false),
  switchReadinessState: () => Promise.resolve(true),
};

describe('ReadinessView', () => {
  it('should render if datacenters readiness data was successfully fetched', () => {
    // given
    vi.mocked(useReadiness).mockReturnValueOnce(useReadinessStub);

    // when
    const { getByText } = render(ReadinessView);

    // then
    expect(vi.mocked(useReadiness)).toHaveBeenCalledOnce();
    expect(getByText('readiness.title')).toBeVisible();
  });

  it('should show loading spinner when fetching Readiness data', () => {
    // given
    vi.mocked(useReadiness).mockReturnValueOnce({
      ...useReadinessStub,
      loading: computed(() => true),
    });

    // when
    const { queryByTestId } = render(ReadinessView);

    // then
    expect(vi.mocked(useReadiness)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeVisible();
  });

  it('should hide loading spinner when data fetch is complete', () => {
    // given
    vi.mocked(useReadiness).mockReturnValueOnce({
      ...useReadinessStub,
      loading: computed(() => false),
    });

    // when
    const { queryByTestId } = render(ReadinessView);

    // then
    expect(vi.mocked(useReadiness)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should show error message when fetching data failed', () => {
    // given
    vi.mocked(useReadiness).mockReturnValueOnce({
      ...useReadinessStub,
      loading: computed(() => false),
      error: ref({ fetchReadiness: new Error() }),
    });

    // when
    const { queryByText } = render(ReadinessView);

    // then
    expect(vi.mocked(useReadiness)).toHaveBeenCalledOnce();
    expect(queryByText('readiness.connectionError.title')).toBeVisible();
    expect(queryByText('readiness.connectionError.text')).toBeVisible();
  });

  it('should not show error message when data was fetch successfully', () => {
    // given
    vi.mocked(useReadiness).mockReturnValueOnce({
      ...useReadinessStub,
      loading: computed(() => false),
      error: ref({ fetchReadiness: null }),
    });

    // when
    const { queryByText } = render(ReadinessView);

    // then
    expect(vi.mocked(useReadiness)).toHaveBeenCalledOnce();
    expect(
      queryByText('readiness.connectionError.title'),
    ).not.toBeInTheDocument();
  });

  it('should show confirmation dialog on switch button click', async () => {
    // given
    vi.mocked(useReadiness).mockReturnValueOnce(useReadinessStub);

    // when
    const { getAllByText, getByText } = render(ReadinessView, {
      testPinia: createTestingPiniaWithState(),
    });
    await fireEvent.click(getAllByText('readiness.turnOn')[0]);

    // then
    expect(
      getByText('readiness.confirmationDialog.switch.title'),
    ).toBeInTheDocument();
    expect(
      getByText('readiness.confirmationDialog.switch.text'),
    ).toBeInTheDocument();
  });
});
