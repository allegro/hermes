import { render } from '@/utils/test-utils';
import ConstraintsBreadcrumbs from '@/views/admin/constraints/constraints-breadcrumbs/ConstraintsBreadcrumbs.vue';

describe('ConstraintsBreadcrumbs', () => {
  it('should render `home` breadcrumb with an anchor to a home page', () => {
    // given
    const { getByText } = render(ConstraintsBreadcrumbs);

    // when
    const element = getByText(
      'constraints.breadcrumbs.home',
    ) as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute('href', '/');
  });
});
