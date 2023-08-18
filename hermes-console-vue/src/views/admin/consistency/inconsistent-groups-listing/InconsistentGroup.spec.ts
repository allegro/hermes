import { consistencyStoreState } from '@/dummy/store';
import { createTestingPinia } from '@pinia/testing';
import {
  dummyGroupInconsistency,
  dummyGroupInconsistency3,
} from '@/dummy/groupInconsistency';
import { render } from '@/utils/test-utils';
import InconsistentGroup from '@/views/admin/consistency/inconsistent-groups-listing/InconsistentGroup.vue';
import router from '@/router';

describe('InconsistentGroup', () => {
  it('should render inconsistent topics table', async () => {
    //given
    const group = 'pl.allegro.public.group';
    const topic = `${group}.DummyEvent`;
    await router.push(`/ui/consistency/${group}`);

    // when
    const { getByText } = render(InconsistentGroup, {
      testPinia: createTestingPinia({
        initialState: {
          consistency: {
            ...consistencyStoreState,
            groups: dummyGroupInconsistency,
          },
        },
        stubActions: false,
      }),
    });

    // then
    expect(getByText(topic)).toBeVisible();
  });

  it('should render empty subscriptions table', async () => {
    //given
    const group = 'pl.allegro.public.group';
    const topic = `${group}.DummyEvent`;
    await router.push(`/ui/consistency/${group}/topics/${topic}`);
    // when
    const { getByText } = render(InconsistentGroup, {
      testPinia: createTestingPinia({
        initialState: {
          consistency: {
            ...consistencyStoreState,
            groups: dummyGroupInconsistency3,
          },
        },
        stubActions: false,
      }),
    });

    // then
    expect(getByText('consistency.inconsistentGroup.noTopics')).toBeVisible();
  });
});
