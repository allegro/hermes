import { dummyGroups } from '@/dummy/groups';
import { fireEvent, waitFor } from '@testing-library/vue';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useGroups } from '@/composables/groups/use-groups/useGroups';
import GroupsView from '@/views/groups/GroupsView.vue';
import type { UseGroups } from '@/composables/groups/use-groups/useGroups';

vi.mock('@/composables/groups/use-groups/useGroups');

const useGroupsStub: UseGroups = {
  groups: ref(dummyGroups),
  loading: ref(false),
  error: ref({
    fetchTopicNames: null,
    fetchGroupNames: null,
  }),
  removeGroup: () => Promise.resolve(true),
};

describe('GroupsView', () => {
  it('should render', () => {
    // given
    vi.mocked(useGroups).mockReturnValueOnce(useGroupsStub);

    // when
    const { getByText } = render(GroupsView);

    // then
    expect(getByText('groups.heading')).toBeInTheDocument();
    expect(getByText('groups.actions.create')).toBeInTheDocument();
  });

  it('should open modal on `new group` button click', async () => {
    // given
    vi.mocked(useGroups).mockReturnValueOnce(useGroupsStub);

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

    // when
    const { queryByText } = render(GroupsView);

    // then
    expect(vi.mocked(useGroups)).toHaveBeenCalledOnce();
    expect(queryByText('groups.connectionError.title')).not.toBeInTheDocument();
  });
});
