import { describe, expect, it } from 'vitest';
import { render, renderWithEmits } from '@/utils/test-utils';
import SearchBar from '@/components/search-commander/search-bar/SearchBar.vue';

describe('SearchBar', () => {
  it('should render the search button', () => {
    // given
    const props = {
      hotKey: 'Ctrl+k',
    };

    // when
    const { getByText } = render(SearchBar, { props });

    // then
    expect(getByText('Search')).toBeVisible();
    expect(getByText('Ctrl')).toBeVisible();
    expect(getByText('+')).toBeVisible();
    expect(getByText('K')).toBeVisible();
  });

  it('should emit an open event when the search button is clicked', async () => {
    // given
    const props = {
      hotKey: 'ctrl+k',
    };
    const wrapper = renderWithEmits(SearchBar, { props });

    // when
    await wrapper.find('[data-testid="search-bar-button"]').trigger('click');

    // then
    expect(wrapper.emitted()).toHaveProperty('open');
  });
});
