import { render } from '@/utils/test-utils';
import ConsistencyBreadcrumbs from '@/views/admin/consistency/consistency-breadcrumbs/ConsistencyBreadcrumbs.vue';

describe('ConsistencyBreadcrumbs', () => {
  it('should render `home` breadcrumb with an anchor to a home page', () => {
    // given
    const { getByText } = render(ConsistencyBreadcrumbs);

    // when
    const element = getByText(
      'consistency.breadcrumbs.home',
    ) as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute('href', '/');
  });
});
