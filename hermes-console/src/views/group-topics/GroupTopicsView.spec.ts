import { createTestingPinia } from '@pinia/testing';
import { createTestingPiniaWithState } from '@/dummy/store';
import {
  dummyDataSources,
  dummyInitializedTopicForm,
  dummyTopicFormValidator,
} from '@/dummy/topic-form';
import { dummyGroups } from '@/dummy/groups';
import { fireEvent } from '@testing-library/vue';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { Role } from '@/api/role';
import { setActivePinia } from 'pinia';
import { useCreateTopic } from '@/composables/topic/use-create-topic/useCreateTopic';
import { useGroups } from '@/composables/groups/use-groups/useGroups';
import { useRoles } from '@/composables/roles/use-roles/useRoles';
import GroupTopicsView from '@/views/group-topics/GroupTopicsView.vue';
import type { UseCreateTopic } from '@/composables/topic/use-create-topic/types';
import type { UseGroups } from '@/composables/groups/use-groups/useGroups';
import type { UseRoles } from '@/composables/roles/use-roles/useRoles';

vi.mock('@/composables/groups/use-groups/useGroups');
vi.mock('@/composables/roles/use-roles/useRoles');

vi.mock('@/composables/topic/use-create-topic/useCreateTopic');

const useCreateTopicStub: UseCreateTopic = {
  form: ref(dummyInitializedTopicForm),
  validators: dummyTopicFormValidator,
  dataSources: dummyDataSources,
  createOrUpdateTopic: () => Promise.resolve(true),
  creatingOrUpdatingTopic: ref(false),
  errors: ref({
    fetchOwners: null,
    fetchOwnerSources: null,
  }),
};

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
  roles: ref([Role.SUBSCRIPTION_OWNER, Role.ADMIN]),
  loading: ref(false),
  error: ref({
    fetchRoles: null,
  }),
};

describe('GroupTopicsView', () => {
  const pinia = createTestingPinia({
    fakeApp: true,
  });

  beforeEach(async () => {
    beforeEach(() => {
      setActivePinia(pinia);
    });
  });

  it('should render if data was successfully fetched', () => {
    // given
    vi.mocked(useGroups).mockReturnValueOnce(useGroupsStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { getByText } = render(GroupTopicsView);

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
    const { queryByTestId } = render(GroupTopicsView);

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
    const { queryByTestId } = render(GroupTopicsView);

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
    const { queryByText } = render(GroupTopicsView);

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
    const { queryByText } = render(GroupTopicsView);

    // then
    expect(vi.mocked(useGroups)).toHaveBeenCalledOnce();
    expect(queryByText('groups.connectionError.title')).not.toBeInTheDocument();
  });

  it('should show confirmation dialog on remove button click', async () => {
    // given
    vi.mocked(useGroups).mockReturnValueOnce(useGroupsStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);

    // when
    const { getByText } = render(GroupTopicsView, {
      testPinia: createTestingPiniaWithState(),
    });
    await fireEvent.click(getByText('groups.actions.remove'));

    // then
    expect(getByText('groups.confirmationDialog.remove.title')).toBeVisible();
    expect(getByText('groups.confirmationDialog.remove.text')).toBeVisible();
  });

  it('should show create topic dialog on button click', async () => {
    // given
    vi.mocked(useGroups).mockReturnValueOnce(useGroupsStub);
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    vi.mocked(useCreateTopic).mockReturnValueOnce(useCreateTopicStub);

    // when
    const { getByText, getAllByText } = render(GroupTopicsView, {
      testPinia: createTestingPiniaWithState(),
    });
    expect(getByText('groups.actions.createTopic')).toBeVisible();
    await fireEvent.click(getByText('groups.actions.createTopic'));

    // then
    // expect(getAllByText('groups.actions.createTopic')[0]).toBeInTheDocument();
    // expect(getByText('topicForm.actions.create')).toBeInTheDocument();
  });
});
