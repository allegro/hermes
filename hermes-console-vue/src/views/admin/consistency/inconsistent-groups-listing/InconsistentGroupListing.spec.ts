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

  it('should render inconsistent groups table', async () => {
    // given
    const group = 'pl.allegro.public.group';

    // when
    const { getByText } = render(InconsistentGroupsListing, {
      props: {
        filter: null,
        inconsistentGroups: dummyGroupInconsistency,
      },
    });

    // then
    expect(getByText(group)).toBeVisible();
  });

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
