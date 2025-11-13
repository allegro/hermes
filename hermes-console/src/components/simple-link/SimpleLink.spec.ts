import { render } from '@/utils/test-utils';
import SimpleLink from './SimpleLink.vue';

describe('SimpleLink', () => {
  it('should render a link with given href and text', () => {
    // given
    const props = {
      href: 'https://example.com',
      text: 'Click me',
    };

    // when
    const { getByRole, getByText } = render(SimpleLink, { props });
    const link = getByRole('link');

    // then
    expect(link).toBeVisible();
    expect(link).toHaveAttribute('href', props.href);
    expect(getByText(props.text)).toBeVisible();
  });

  it('should render a link that opens in a new tab', () => {
    // given
    const props = {
      href: 'https://example.com',
      text: 'Click me',
      openInNewTab: true,
    };

    // when
    const { getByRole, container } = render(SimpleLink, { props });
    const link = getByRole('link');
    const icon = container.querySelector('.mdi-open-in-new');

    // then
    expect(link).toHaveAttribute('target', '_blank');
    expect(icon).toBeVisible();
  });

  it('should render a link that opens in the same tab', () => {
    // given
    const props = {
      href: 'https://example.com',
      text: 'Click me',
      openInNewTab: false,
    };

    // when
    const { getByRole, queryByText } = render(SimpleLink, { props });
    const link = getByRole('link');
    const icon = queryByText('mdi-open-in-new');

    // then
    expect(link).toHaveAttribute('target', '_self');
    expect(icon).toBeNull();
  });

  it('should render a link that opens in the same tab by default', () => {
    // given
    const props = {
      href: 'https://example.com',
      text: 'Click me',
    };

    // when
    const { getByRole, queryByText } = render(SimpleLink, { props });
    const link = getByRole('link');
    const icon = queryByText('mdi-open-in-new');

    // then
    expect(link).toHaveAttribute('target', '_self');
    expect(icon).toBeNull();
  });
});
