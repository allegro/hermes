import { consistencyStoreState } from '@/dummy/store';
import { createTestingPinia } from '@pinia/testing';
import { describe, expect } from 'vitest';
import {
  dummyGroupInconsistency,
  dummyGroupInconsistency2,
} from '@/dummy/groupInconsistency';
import { render } from '@/utils/test-utils';
import InconsistentTopic from '@/views/admin/consistency/inconsistent-topic/InconsistentTopic.vue';
import router from '@/router';

describe('InconsistentTopic', () => {
  it('should render inconsistent subscriptions table', async () => {
    //given
    const group = 'pl.allegro.public.group';
    const topic = `${group}.DummyEvent`;
    const subscription = `${topic}$foobar-service`;
    await router.push(`/ui/consistency/${group}/topics/${topic}`);

    // when
    const { getByText } = render(InconsistentTopic, {
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
    expect(getByText(subscription)).toBeVisible();
  });

  it('should render empty subscriptions table', async () => {
    //given
    const group = 'pl.allegro.public.group';
    const topic = `${group}.DummyEvent`;
    await router.push(`/ui/consistency/${group}/topics/${topic}`);

    // when
    const { getByText } = render(InconsistentTopic, {
      testPinia: createTestingPinia({
        initialState: {
          consistency: {
            ...consistencyStoreState,
            groups: dummyGroupInconsistency2,
          },
        },
        stubActions: false,
      }),
    });

    // then
    expect(
      getByText(
        'consistency.inconsistentGroup.inconsistentTopic.noSubscriptions',
      ),
    ).toBeVisible();
  });
});
