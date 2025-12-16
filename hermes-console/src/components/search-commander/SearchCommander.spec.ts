import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { dummySearchResults } from '@/dummy/search';
import { fireEvent, waitFor, within } from '@testing-library/vue';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useRouter } from 'vue-router';
import { useSearch, type UseSearch } from '@/composables/search-v2/useSearchV2';
import SearchCommander from '@/components/search-commander/SearchCommander.vue';

vi.mock('@/composables/search-v2/useSearchV2');
vi.mock('vue-router', () => ({
  useRouter: vi.fn(),
  createRouter: vi.fn(() => ({
    beforeEach: vi.fn(),
  })),
  createWebHistory: vi.fn(),
}));
vi.mock('@');

const useSearchV2Mock: UseSearch = {
  results: ref(null),
  loading: ref(false),
  error: ref({ fetchError: null }),
  runSearch: vi.fn(),
};

describe('SearchCommander', () => {
  const mockPush = vi.fn();

  beforeEach(() => {
    vi.mocked(useRouter).mockReturnValue({
      push: mockPush,
    } as any);
    vi.mocked(useSearch).mockReturnValue(useSearchV2Mock);
  });

  afterEach(() => {
    vi.useRealTimers();
    mockPush.mockClear();
    vi.mocked(useSearchV2Mock.runSearch).mockClear();
    useSearchV2Mock.results.value = null;
    useSearchV2Mock.loading.value = false;
    useSearchV2Mock.error.value = { fetchError: null };
  });

  const props = {
    modelValue: true,
    'onUpdate:modelValue': vi.fn(),
  };

  it('should render search input', () => {
    // given
    const { getByPlaceholderText } = render(SearchCommander, {
      props,
    });

    // expect
    expect(
      getByPlaceholderText('searchCommander.searchInputPlaceholder'),
    ).toBeVisible();
  });

  it('should not call search if query is too short', async () => {
    // given
    vi.useFakeTimers();
    const { getByPlaceholderText } = render(SearchCommander, {
      props,
    });
    const searchInput = getByPlaceholderText(
      'searchCommander.searchInputPlaceholder',
    );

    // when
    await fireEvent.update(searchInput, 'ab');
    await vi.advanceTimersByTimeAsync(400);

    // then
    expect(vi.mocked(useSearchV2Mock.runSearch)).not.toHaveBeenCalled();
    vi.useRealTimers();
  });

  it('should call search if query is long enough', async () => {
    // given
    vi.useFakeTimers();
    const { getByPlaceholderText } = render(SearchCommander, {
      props,
    });
    const searchInput = getByPlaceholderText(
      'searchCommander.searchInputPlaceholder',
    );

    // when
    await fireEvent.update(searchInput, 'abc');
    await vi.advanceTimersByTimeAsync(300);

    // then
    expect(vi.mocked(useSearchV2Mock.runSearch)).toHaveBeenCalledWith('abc');
    vi.useRealTimers();
  });

  it('should render loading indicator', () => {
    // given
    useSearchV2Mock.loading.value = true;
    const { getByRole } = render(SearchCommander, {
      props,
    });

    // expect
    expect(getByRole('progressbar')).toBeVisible();
  });

  it('should render results', async () => {
    // given
    vi.useFakeTimers();
    const { getByPlaceholderText, findAllByTestId } = render(SearchCommander, {
      props,
    });
    const searchInput = getByPlaceholderText(
      'searchCommander.searchInputPlaceholder',
    );

    // when
    await fireEvent.update(searchInput, 'DummyEvent');
    useSearchV2Mock.results.value = dummySearchResults;
    await vi.advanceTimersByTimeAsync(300);

    // then
    await waitFor(async () => {
      const elements = await findAllByTestId('command-palette-element');
      expect(elements).toHaveLength(6);
      expect(
        within(elements[0]).getByText('searchCommander.sections.topics'),
      ).toBeVisible();
      expect(
        within(elements[1]).getByText('pl.allegro.public.group.DummyEvent'),
      ).toBeVisible();
      expect(
        within(elements[2]).getByTestId('command-palette-divider-element'),
      ).toBeVisible();
      expect(
        within(elements[3]).getByText('searchCommander.sections.subscriptions'),
      ).toBeVisible();
      expect(within(elements[4]).getByText('foobar-service')).toBeVisible();
      expect(within(elements[5]).getByText('barbaz-service')).toBeVisible();
    });
  });

  it('should navigate to topic on click', async () => {
    // given
    const { getByPlaceholderText, findAllByText } = render(SearchCommander, {
      props,
    });
    const searchInput = getByPlaceholderText(
      'searchCommander.searchInputPlaceholder',
    );
    await fireEvent.update(searchInput, 'DummyEvent');
    useSearchV2Mock.results.value = dummySearchResults;

    // when
    const topic = (
      await findAllByText('pl.allegro.public.group.DummyEvent')
    )[0];
    await fireEvent.click(topic);

    // then
    expect(mockPush).toHaveBeenCalledWith({
      name: 'topic',
      params: {
        groupId: 'pl.allegro.public.group',
        topicName: 'pl.allegro.public.group.DummyEvent',
      },
    });
  });

  it('should navigate to subscription on click', async () => {
    // given
    const { getByPlaceholderText, findByText } = render(SearchCommander, {
      props,
    });
    const searchInput = getByPlaceholderText(
      'searchCommander.searchInputPlaceholder',
    );

    // when
    await fireEvent.update(searchInput, 'test-subscription');
    useSearchV2Mock.results.value = dummySearchResults;
    const subscription = await findByText('foobar-service');
    await fireEvent.click(subscription);

    // then
    expect(mockPush).toHaveBeenCalledWith({
      name: 'subscription',
      params: {
        groupId: 'pl.allegro.public.group',
        topicId: 'pl.allegro.public.group.DummyEvent',
        subscriptionId: 'foobar-service',
      },
    });
  });

  it('should show no results message', async () => {
    // given
    vi.useFakeTimers();
    const { getByPlaceholderText, findByText } = render(SearchCommander, {
      props,
    });
    const searchInput = getByPlaceholderText(
      'searchCommander.searchInputPlaceholder',
    );

    // when
    await fireEvent.update(searchInput, 'abc');
    await vi.advanceTimersByTimeAsync(300);
    useSearchV2Mock.results.value = {
      totalCount: 0,
      results: [],
    };

    // then
    expect(useSearchV2Mock.runSearch).toHaveBeenCalledWith('abc');
    expect(await findByText('commandPalette.noResults')).toBeVisible();
    vi.useRealTimers();
  });

  it('should emit update:modelValue on close', async () => {
    // given
    const onUpdate = vi.fn();
    const { getByPlaceholderText, findByText } = render(SearchCommander, {
      props: {
        modelValue: true,
        'onUpdate:modelValue': onUpdate,
      },
    });
    const searchInput = getByPlaceholderText(
      'searchCommander.searchInputPlaceholder',
    );

    // when
    await fireEvent.update(searchInput, 'test-topic');
    useSearchV2Mock.results.value = dummySearchResults;
    const topic = await findByText('pl.allegro.public.group.DummyEvent');
    await fireEvent.click(topic);

    // then
    expect(onUpdate).toHaveBeenCalledWith(false);
  });
});
