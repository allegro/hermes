import { dummyGroups } from '@/dummy/groups';
import { dummyRoles } from '@/dummy/roles';
import { fireEvent, waitFor } from '@testing-library/vue';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useGroups } from '@/composables/groups/use-groups/useGroups';
import { useRoles } from '@/composables/roles/use-roles/useRoles';
import GroupsView from '@/views/groups/GroupsView.vue';
import type { UseGroups } from '@/composables/groups/use-groups/useGroups';
import type { UseRoles } from '@/composables/roles/use-roles/useRoles';

vi.mock('@/composables/groups/use-groups/useGroups');
vi.mock('@/composables/roles/use-roles/useRoles');

const useGroupsStub: UseGroups = {
  groups: ref(dummyGroups),
  loading: ref(false),
  error: ref({
    fetchTopicNames: null,
    fetchGroupNames: null,
  }),
  removeGroup: () => Promise.resolve(true),
  createGroup: () => Promise.resolve(true),
};

const useRolesStub: UseRoles = {
  roles: ref(dummyRoles),
  loading: ref(false),
  error: ref({
    fetchRoles: null,
  }),
};

describe('GroupsView', () => {
  it('should render', () => {
    // given
    vi.mocked(useGroups).mockReturnValueOnce(useGroupsStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { getByText } = render(GroupsView);

    // then
    expect(getByText('groups.heading')).toBeInTheDocument();
    expect(getByText('groups.actions.create')).toBeInTheDocument();
  });

  it('should open modal on `new group` button click', async () => {
    // given
    vi.mocked(useGroups).mockReturnValueOnce(useGroupsStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // given
    const { getByText } = render(GroupsView);

    // when
    await fireEvent.click(getByText('groups.actions.create')!);

    // then
    await waitFor(() => {
      expect(getByText('groups.groupForm.createTitle')).toBeInTheDocument();
    });
  });

  it('should render if data was successfully fetched', () => {
    // given
    vi.mocked(useGroups).mockReturnValueOnce(useGroupsStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { getByText } = render(GroupsView);

    // then
    expect(vi.mocked(useGroups)).toHaveBeenCalledOnce();
    expect(getByText('groups.actions.search')).toBeVisible();
  });

  it('should show loading spinner when fetching data', () => {
    // given
    vi.mocked(useGroups).mockReturnValueOnce({
      ...useGroupsStub,
      loading: ref(true),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByTestId } = render(GroupsView);

    // then
    expect(vi.mocked(useGroups)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).toBeVisible();
  });

  it('should hide loading spinner when data fetch is complete', () => {
    // given
    vi.mocked(useGroups).mockReturnValueOnce({
      ...useGroupsStub,
      loading: ref(false),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByTestId } = render(GroupsView);

    // then
    expect(vi.mocked(useGroups)).toHaveBeenCalledOnce();
    expect(queryByTestId('loading-spinner')).not.toBeInTheDocument();
  });

  it('should show error message when fetching data failed', () => {
    // given
    vi.mocked(useGroups).mockReturnValueOnce({
      ...useGroupsStub,
      loading: ref(false),
      error: ref({ fetchTopicNames: new Error(), fetchGroupNames: null }),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByText } = render(GroupsView);

    // then
    expect(vi.mocked(useGroups)).toHaveBeenCalledOnce();
    expect(queryByText('groups.connectionError.title')).toBeVisible();
    expect(queryByText('groups.connectionError.text')).toBeVisible();
  });

  it('should not show error message when data was fetch successfully', () => {
    // given
    vi.mocked(useGroups).mockReturnValueOnce({
      ...useGroupsStub,
      loading: ref(false),
      error: ref({ fetchTopicNames: null, fetchGroupNames: null }),
    });
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { queryByText } = render(GroupsView);

    // then
    expect(vi.mocked(useGroups)).toHaveBeenCalledOnce();
    expect(queryByText('groups.connectionError.title')).not.toBeInTheDocument();
  });
});
