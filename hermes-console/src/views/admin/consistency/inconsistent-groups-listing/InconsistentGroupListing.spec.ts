import { beforeEach } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { dummyGroupInconsistency } from '@/dummy/groupInconsistency';
import { render } from '@/utils/test-utils';
import InconsistentGroupsListing from '@/views/admin/consistency/inconsistent-groups-listing/InconsistentGroupsListing.vue';
import router from '@/router';

describe('InconsistentGroupListing', () => {
  beforeEach(async () => {
    setActivePinia(createPinia());
  });

  it.each([
    [null, 1],
    ['pl.allegro.public.group', 1],
    ['PL.ALLEGRO.PUBLIC.GROUP', 1],
    ['public', 1],
    ['Public', 1],
    ['pl.allegro.internal.group', 0],
    ['FOO', 0],
  ])(
    'should render inconsistent groups table with filter applied (case-insensitive, filter: %s)',
    async (filter: string | null, expectedGroups: number) => {
      // given
      const group = 'pl.allegro.public.group';

      // when
      const { queryAllByText } = render(InconsistentGroupsListing, {
        props: {
          filter,
          inconsistentGroups: dummyGroupInconsistency,
        },
      });

      // then
      expect(queryAllByText(group).length).toBe(expectedGroups);
    },
  );

  it('should render empty groups table', async () => {
    //given
    const group = 'pl.allegro.public.group';
    const topic = `${group}.DummyEvent`;
    await router.push(`/ui/consistency/${group}/topics/${topic}`);

    // when
    const { getByText } = render(InconsistentGroupsListing, {
      props: {
        filter: null,
        inconsistentGroups: [],
      },
    });

    // then
    expect(getByText('consistency.inconsistentGroups.noGroups')).toBeVisible();
  });
});
