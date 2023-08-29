import { fireEvent } from '@testing-library/vue';
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

  it('should disable save button for invalid group name', async () => {
    // given
    const props = {
      dialogOpen: true,
      operation: 'create',
    };

    // when
    const { getByText } = render(GroupForm, { props });

    // then
    expect(getByText('groups.groupForm.save').closest('button')).toBeDisabled();
  });

  it('should enable save button for valid group name', async () => {
    // given
    const props = {
      dialogOpen: true,
      operation: 'create',
    };

    // when
    const { getByLabelText, getByText } = render(GroupForm, { props });

    await fireEvent.update(
      getByLabelText('groups.groupForm.groupName'),
      'pl.allegro.sample',
    );

    // then
    expect(
      getByText('groups.groupForm.save').closest('button'),
    ).not.toBeEnabled();
  });
});
