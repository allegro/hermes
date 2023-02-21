import { render } from '@/utils/test-utils';
import GroupBreadcrumbs from '@/views/groups/group-breadcrumbs/GroupBreadcrumbs.vue';

describe('GroupBreadcrumbs', () => {
  it('should render `home` breadcrumb with an anchor to a home page', () => {
    // given
    const { getByText } = render(GroupBreadcrumbs);

    // when
    const element = getByText(
      'groups.groupBreadcrumbs.home',
    ) as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute('href', '/');
  });
});
