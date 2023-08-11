import { fireEvent, waitFor } from '@testing-library/vue';
import { render } from '@/utils/test-utils';
import GroupForm from '@/views/groups/group-form/GroupForm.vue';

describe('GroupForm', () => {
  it('should render form (create variant)', () => {
    // given
    const props = {
      dialogOpen: true,
      operation: 'create',
    };

    // when
    const { queryByText } = render(GroupForm, { props });

    // then
    expect(queryByText('groups.groupForm.createTitle')).toBeInTheDocument();
    expect(queryByText('groups.groupForm.edu')).toBeInTheDocument();
  });

  it('should render form (edit variant)', () => {
    // given
    const props = {
      dialogOpen: true,
      operation: 'edit',
    };

    // when
    const { queryByText } = render(GroupForm, { props });

    // then
    expect(queryByText('groups.groupForm.editTitle')).toBeInTheDocument();
    expect(queryByText('groups.groupForm.edu')).not.toBeInTheDocument();
  });

  it('should not show name validation error for untouched form', () => {
    // given
    const props = {
      dialogOpen: true,
      operation: 'create',
    };

    // when
    const { queryByText } = render(GroupForm, { props });

    // then
    expect(
      queryByText('groups.groupForm.validation.groupName'),
    ).not.toBeInTheDocument();
  });

  it('should show name validation error after invalid form submit', async () => {
    // given
    const props = {
      dialogOpen: true,
      operation: 'create',
    };

    // when
    const { getByText } = render(GroupForm, { props });
    await fireEvent.click(getByText('groups.groupForm.save'));

    // then
    await waitFor(() => {
      expect(
        getByText('groups.groupForm.validation.groupName'),
      ).toBeInTheDocument();
    });
  });

  it('should not show name validation error for valid group name', async () => {
    // given
    const props = {
      dialogOpen: true,
      operation: 'create',
    };

    // when
    const { getByLabelText, queryByText } = render(GroupForm, { props });

    await fireEvent.update(
      getByLabelText('groups.groupForm.groupName'),
      'pl.allegro.sample',
    );
    await fireEvent.click(queryByText('groups.groupForm.save')!);

    // then
    expect(
      queryByText('groups.groupForm.validation.groupName'),
    ).not.toBeInTheDocument();
  });
});
