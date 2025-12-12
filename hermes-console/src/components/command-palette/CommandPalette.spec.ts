import { describe, expect, it, vi } from 'vitest';
import { fireEvent } from '@testing-library/vue';
import { render, renderWithEmits } from '@/utils/test-utils';
import CommandPalette from '@/components/command-palette/CommandPalette.vue';

describe('CommandPalette', () => {
  const items = [
    {
      id: '1',
      type: 'subheader',
      title: 'Group 1',
    },
    {
      id: '2',
      type: 'item',
      title: 'Item 1',
      subtitle: 'Subtitle 1',
      onClick: vi.fn(),
    },
    {
      id: '3',
      type: 'divider',
    },
    {
      id: '4',
      type: 'subheader',
      title: 'Group 2',
    },
    {
      id: '5',
      type: 'item',
      title: 'Item 2',
      onClick: vi.fn(),
    },
  ];

  it('should render search input', () => {
    // given
    const { getByPlaceholderText } = render(CommandPalette, {
      props: {
        items: [],
        numberOfResults: 0,
        search: '',
        loading: false,
        modelValue: true,
        inputPlaceholder: 'Search topics and subscriptions',
      },
    });

    // expect
    expect(
      getByPlaceholderText('Search topics and subscriptions'),
    ).toBeVisible();
  });

  it('should render loading indicator', () => {
    // given
    const { getByRole } = render(CommandPalette, {
      props: {
        items: [],
        numberOfResults: 0,
        search: '',
        loading: true,
        modelValue: true,
      },
    });

    // expect
    expect(getByRole('progressbar')).toBeVisible();
  });

  it('should render no results message', () => {
    // given
    const { getByText } = render(CommandPalette, {
      props: {
        items: [],
        numberOfResults: 0,
        search: 'test',
        loading: false,
        modelValue: true,
      },
    });

    // expect
    expect(getByText('commandPalette.noResults')).toBeVisible();
  });

  it('should render search incentive message', () => {
    // given
    const { getByText } = render(CommandPalette, {
      props: {
        items: [],
        numberOfResults: 0,
        search: '',
        loading: false,
        modelValue: true,
      },
    });

    // expect
    expect(getByText('commandPalette.searchIncentive')).toBeVisible();
  });

  it('should render items', () => {
    // given
    const { getByText } = render(CommandPalette, {
      props: {
        items,
        numberOfResults: 2,
        search: 'test',
        loading: false,
        modelValue: true,
      },
    });

    // expect
    expect(getByText('Group 1')).toBeVisible();
    expect(getByText('Item 1')).toBeVisible();
    expect(getByText('Subtitle 1')).toBeVisible();
    expect(getByText('Group 2')).toBeVisible();
    expect(getByText('Item 2')).toBeVisible();
  });

  it('should emit update:search on search input', async () => {
    // given
    const wrapper = renderWithEmits(CommandPalette, {
      props: {
        items: [],
        numberOfResults: 0,
        search: '',
        loading: false,
        modelValue: true,
        inputPlaceholder: 'Search topics and subscriptions',
      },
    });

    // when
    const input = document.body.querySelector(
      'input[placeholder="Search topics and subscriptions"]',
    ) as HTMLInputElement;
    await fireEvent.update(input, 'new value');

    // then
    expect(wrapper.emitted()['update:search'][0]).toEqual(['new value']);
  });

  it('should call action on item click', async () => {
    // given
    const { getByText } = render(CommandPalette, {
      props: {
        items,
        numberOfResults: 2,
        search: 'test',
        loading: false,
        modelValue: true,
      },
    });

    // when
    await fireEvent.click(getByText('Item 1'));

    // then
    const item = items.find((it) => it.id === '2')!!;
    expect(vi.mocked(item.onClick)).toHaveBeenCalled();
  });
});
