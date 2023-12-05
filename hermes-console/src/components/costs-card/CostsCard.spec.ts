import { describe } from 'vitest';
import { render } from '@/utils/test-utils';
import CostsCard from '@/components/costs-card/CostsCard.vue';

describe('CostsCard', () => {
  const props = {
    iframeUrl:
      'https://www.openstreetmap.org/export/embed.html?bbox=-0.004017949104309083%2C51.47612752641776%2C0.00030577182769775396%2C51.478569861898606&layer=mapnik',
    detailsUrl: 'https://www.openstreetmap.org',
  };

  it('should render title properly', () => {
    // when
    const { getByText } = render(CostsCard, { props });

    // then
    const row = getByText('costsCard.title');
    expect(row).toBeVisible();
  });

  it('should render iframe properly', async () => {
    // when
    const { container } = render(CostsCard, { props });
    const element = container.querySelector('iframe')!!;

    // then
    expect(element).toBeVisible();
    expect(element).toHaveAttribute('src', props.iframeUrl);
  });
});
