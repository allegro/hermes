import { render } from '@/utils/test-utils';
import ReadinessBreadcrumbs from '@/views/admin/readiness/readiness-breadcrumbs/ReadinessBreadcrumbs.vue';

describe('ReadinessBreadcrumbs', () => {
  it('should render `home` breadcrumb with an anchor to a home page', () => {
    // given
    const { getByText } = render(ReadinessBreadcrumbs);

    // when
    const element = getByText(
      'readiness.breadcrumbs.home',
    ) as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute('href', '/');
  });
});
