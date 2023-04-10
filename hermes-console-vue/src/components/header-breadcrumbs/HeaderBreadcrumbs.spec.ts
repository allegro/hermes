import { render } from '@/utils/test-utils';
import HeaderBreadcrumbs from '@/components/header-breadcrumbs/HeaderBreadcrumbs.vue';
import { expect } from 'vitest';

describe('HeaderBreadcrumbs', () => {
  const props = {
    items: [
      {
        title: 'home',
        href: '/',
      },
      {
        title: 'group',
      },
    ],
  };

  it('should render `home` breadcrumb with an anchor to a home page', () => {
    // given
    const { getByText } = render(HeaderBreadcrumbs, { props });

    // when
    const homeElement = getByText('home') as HTMLAnchorElement;

    // then
    expect(homeElement).toHaveAttribute('href', '/');

    // and
    const groupElement = getByText('group') as HTMLAnchorElement;

    // then
    expect(groupElement).to.exist;
  });
});
