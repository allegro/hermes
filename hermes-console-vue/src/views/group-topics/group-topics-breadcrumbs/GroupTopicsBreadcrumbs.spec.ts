import { render } from '@/utils/test-utils';
import GroupTopicsBreadcrumbs from '@/views/group-topics/group-topics-breadcrumbs/GroupTopicsBreadcrumbs.vue';

describe('GroupTopicsBreadcrumbs', () => {
  it('should render `home` breadcrumb with an anchor to a home page', () => {
    // given
    const { getByText } = render(GroupTopicsBreadcrumbs);

    // when
    const element = getByText(
      'groupTopics.groupTopicsBreadcrumbs.home',
    ) as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute('href', '/');
  });

  it('should render `group` breadcrumb with anchor to groups listing', () => {
    // given
    const { getByText } = render(GroupTopicsBreadcrumbs);

    // when
    const element = getByText(
      'groupTopics.groupTopicsBreadcrumbs.groups',
    ) as HTMLAnchorElement;

    // then
    expect(element).toHaveAttribute('href', '/groups/');
  });
});
