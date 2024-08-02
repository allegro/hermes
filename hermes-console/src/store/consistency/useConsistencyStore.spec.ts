import { beforeEach, describe, expect } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { dummyGroupInconsistency } from '@/dummy/groupInconsistency';
import { dummyInconsistentGroups } from '@/dummy/inconsistentGroups';
import {
  fetchConsistencyGroupsErrorHandler,
  fetchConsistencyGroupsHandler,
  fetchGroupInconsistenciesErrorHandler,
  fetchGroupInconsistenciesHandler,
} from '@/mocks/handlers';
import { setupServer } from 'msw/node';
import { useConsistencyStore } from '@/store/consistency/useConsistencyStore';

describe('useConsistencyStore', () => {
  const server = setupServer();

  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('should initialize', async () => {
    // when
    const consistencyStore = useConsistencyStore();

    // then
    expect(consistencyStore.groups).toEqual([]);
  });

  it('should fetch group consistency', async () => {
    server.use(
      fetchConsistencyGroupsHandler({ groups: dummyInconsistentGroups }),
      fetchGroupInconsistenciesHandler({
        groupsInconsistency: dummyGroupInconsistency,
      }),
    );
    server.listen();
    const consistencyStore = useConsistencyStore();
    await consistencyStore.fetch();

    expect(consistencyStore.groups).toEqual(dummyGroupInconsistency);
    expect(consistencyStore.error.fetchError).toBeNull();
  });

  it('should filter groups', async () => {
    const consistencyStore = useConsistencyStore();
    consistencyStore.groups = dummyGroupInconsistency;

    expect(consistencyStore.group(dummyGroupInconsistency[0].name)).toEqual(
      dummyGroupInconsistency[0],
    );
    expect(consistencyStore.group('whatever')).toBeUndefined();
  });

  it('should filter topics', async () => {
    const consistencyStore = useConsistencyStore();
    consistencyStore.groups = dummyGroupInconsistency;

    expect(
      consistencyStore.topic(
        dummyGroupInconsistency[0].name,
        dummyGroupInconsistency[0].inconsistentTopics[0].name,
      ),
    ).toEqual(dummyGroupInconsistency[0].inconsistentTopics[0]);
    expect(consistencyStore.topic('whatever', 'topic')).toBeUndefined();
  });

  it('should set error when fetching group inconsistencies fails', async () => {
    server.use(
      fetchConsistencyGroupsHandler({ groups: dummyInconsistentGroups }),
      fetchGroupInconsistenciesErrorHandler({}),
    );
    server.listen();
    const consistencyStore = useConsistencyStore();
    await consistencyStore.fetch();

    expect(consistencyStore.error.fetchError).not.toBeNull();
  });
});
