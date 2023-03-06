import { fireEvent, waitFor } from '@testing-library/vue';
import { render } from '@/utils/test-utils';
import GroupsView from '@/views/groups/GroupsView.vue';

describe('GroupsView', () => {
  it('should render', () => {
    // when
    const { queryByText } = render(GroupsView);

    // then
    expect(queryByText('groups.heading')).toBeInTheDocument();
    expect(queryByText('groups.groupForm.createTitle')).not.toBeInTheDocument();
  });

  it('should open modal on `new group` button click', async () => {
    // given
    const { queryByText } = render(GroupsView);

    // when
    await fireEvent.click(queryByText('groups.actions.create')!);

    // then
    await waitFor(() => {
      expect(queryByText('groups.groupForm.createTitle')).toBeInTheDocument();
    });
  });
});
