import { createTestingPiniaWithState } from '@/dummy/store';
import { dummyRoles } from '@/dummy/roles';
import { expect } from 'vitest';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { Role } from '@/api/role';
import { useRoles } from '@/composables/roles/use-roles/useRoles';
import HomeView from '@/views/home/HomeView.vue';
import type { UseRoles } from '@/composables/roles/use-roles/useRoles';

vi.mock('@/composables/roles/use-roles/useRoles');

const useRolesStub: UseRoles = {
  roles: ref(dummyRoles),
  loading: ref(false),
  error: ref({
    fetchRoles: null,
  }),
};

describe('HomeView', () => {
  it('renders properly without admin tools', () => {
    // when
    vi.mocked(useRoles).mockReturnValueOnce(useRolesStub);
    const { getByText, queryByText } = render(HomeView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(vi.mocked(useRoles)).toHaveBeenCalledOnce();
    expect(getByText('homeView.links.console')).toBeVisible();
    expect(queryByText('homeView.links.adminTools')).not.toBeInTheDocument();
  });

  it.each([
    { text: 'homeView.links.console' },
    { text: 'homeView.links.runtime' },
    { text: 'homeView.links.statistics' },
    { text: 'homeView.links.search' },
  ])('should render proper buttons', ({ text }) => {
    // when
    const { getByText } = render(HomeView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(vi.mocked(useRoles)).toHaveBeenCalledOnce();
    expect(getByText(text)).toBeVisible();
  });

  it('renders properly with admin tools', () => {
    // when
    vi.mocked(useRoles).mockReturnValueOnce({
      ...useRolesStub,
      roles: ref([Role.ADMIN]),
    });
    const { getByText } = render(HomeView, {
      testPinia: createTestingPiniaWithState(),
    });

    // then
    expect(vi.mocked(useRoles)).toHaveBeenCalledOnce();
    expect(getByText('homeView.links.adminTools')).toBeVisible();
  });
});
