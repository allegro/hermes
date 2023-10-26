import { ContentType } from '@/api/content-type';
import { describe } from 'vitest';
import { dummyTopic } from '@/dummy/topic';
import { fireEvent } from '@testing-library/vue';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useSubscriptionFiltersDebug } from '@/composables/subscription/use-subscription-filters-debug/useSubscriptionFiltersDebug';
import { VerificationStatus } from '@/api/message-filters-verification';
import SubscriptionPathFiltersDebug from '@/views/subscription/subscription-form/subscription-basic-filters/SubscriptionPathFiltersDebug.vue';
import type { UseSubscriptionFiltersDebug } from '@/composables/subscription/use-subscription-filters-debug/useSubscriptionFiltersDebug';

vi.mock(
  '@/composables/subscription/use-subscription-filters-debug/useSubscriptionFiltersDebug',
);

const useSubscriptionFiltersDebugStub: UseSubscriptionFiltersDebug = {
  status: ref(),
  errorMessage: ref(),
  fetchContentType: () =>
    Promise.resolve({
      contentType: ContentType.AVRO,
      error: null,
    }),
  verify: () => Promise.resolve(),
};

describe('SubscriptionPathFiltersDebug', () => {
  it('should not open debug dialog when fetching topic content type fails', async () => {
    // given
    vi.mocked(useSubscriptionFiltersDebug).mockReturnValueOnce({
      ...useSubscriptionFiltersDebugStub,
      fetchContentType: () =>
        Promise.resolve({
          contentType: undefined,
          error: Error(),
        }),
    });
    // when
    const { getByTestId, queryByText } = render(SubscriptionPathFiltersDebug, {
      props: {
        modelValue: [],
        topic: dummyTopic.name,
        editEnabled: true,
      },
    });

    expect(vi.mocked(useSubscriptionFiltersDebug)).toHaveBeenCalledOnce();
    await fireEvent.click(getByTestId('openFilterDebugButton'));

    // then
    expect(queryByText('filterDebug.title')).not.toBeInTheDocument();
  });

  it('should open debug dialog when fetching topic content type succeeds', async () => {
    // given
    vi.mocked(useSubscriptionFiltersDebug).mockReturnValueOnce({
      ...useSubscriptionFiltersDebugStub,
    });
    // when
    const { getByTestId, queryByText } = render(SubscriptionPathFiltersDebug, {
      props: {
        modelValue: [],
        topic: dummyTopic.name,
        editEnabled: true,
      },
    });

    expect(vi.mocked(useSubscriptionFiltersDebug)).toHaveBeenCalledOnce();
    await fireEvent.click(getByTestId('openFilterDebugButton'));

    // then
    expect(queryByText('filterDebug.title')).toBeVisible();
  });

  it('should not display "update subscription filters" button when edit is disabled', async () => {
    // given
    vi.mocked(useSubscriptionFiltersDebug).mockReturnValueOnce(
      useSubscriptionFiltersDebugStub,
    );
    // when
    const { getByTestId, queryByText } = render(SubscriptionPathFiltersDebug, {
      props: {
        modelValue: [],
        topic: dummyTopic.name,
        editEnabled: false,
      },
    });

    expect(vi.mocked(useSubscriptionFiltersDebug)).toHaveBeenCalledOnce();
    await fireEvent.click(getByTestId('openFilterDebugButton'));

    // then
    expect(queryByText('filterDebug.saveButton')).not.toBeInTheDocument();
  });

  it('should display "update subscription filters" button when edit is enabled', async () => {
    // given
    vi.mocked(useSubscriptionFiltersDebug).mockReturnValueOnce(
      useSubscriptionFiltersDebugStub,
    );
    // when
    const { getByTestId, queryByText } = render(SubscriptionPathFiltersDebug, {
      props: {
        modelValue: [],
        topic: dummyTopic.name,
        editEnabled: true,
      },
    });

    expect(vi.mocked(useSubscriptionFiltersDebug)).toHaveBeenCalledOnce();
    await fireEvent.click(getByTestId('openFilterDebugButton'));

    // then
    expect(queryByText('filterDebug.saveButton')).toBeVisible();
  });

  it('should display verification result', async () => {
    vi.mocked(useSubscriptionFiltersDebug).mockReturnValueOnce({
      ...useSubscriptionFiltersDebugStub,
      status: ref(VerificationStatus.ERROR),
      errorMessage: ref('Invalid json'),
    });
    // when
    const { getByTestId, queryByText } = render(SubscriptionPathFiltersDebug, {
      props: {
        modelValue: [],
        topic: dummyTopic.name,
        editEnabled: true,
      },
    });

    expect(vi.mocked(useSubscriptionFiltersDebug)).toHaveBeenCalledOnce();
    await fireEvent.click(getByTestId('openFilterDebugButton'));

    // then
    expect(queryByText('filterDebug.error')).toBeVisible();
    expect(queryByText('Invalid json')).toBeVisible();
  });
});
