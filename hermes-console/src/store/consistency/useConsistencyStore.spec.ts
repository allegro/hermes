import { beforeEach, describe, expect } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import {
  dummyGroupInconsistency,
  dummyGroupInconsistency2,
  dummyGroupInconsistency4,
} from '@/dummy/groupInconsistency';
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
    expect(consistencyStore.progressPercent).toEqual(0);
    expect(consistencyStore.fetchInProgress).toBeFalsy();
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

  it('should set error when fetching consistency groups fails', async () => {
    server.use(fetchConsistencyGroupsErrorHandler({}));
    server.listen();
    const consistencyStore = useConsistencyStore();
    await consistencyStore.fetch();

    expect(consistencyStore.error.fetchError).not.toBeNull();
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

  it('should remove group from store when refresh returns that it is consistent', async () => {
    // given
    server.use(fetchGroupInconsistenciesHandler({ groupsInconsistency: [] }));
    server.listen();
    const consistencyStore = useConsistencyStore();
    consistencyStore.groups = dummyGroupInconsistency4;
    // make a copy of initial state
    const expected = JSON.parse(JSON.stringify(dummyGroupInconsistency4[1]));

    // when
    await consistencyStore.refresh(dummyGroupInconsistency4[0].name);

    // then
    expect(consistencyStore.groups.length).toEqual(1);
    expect(consistencyStore.groups[0]).toEqual(expected);
  });

  it('should update group in store when refresh returns different value', async () => {
    // given
    server.use(
      fetchGroupInconsistenciesHandler({
        groupsInconsistency: dummyGroupInconsistency2,
      }),
    );
    server.listen();
    const consistencyStore = useConsistencyStore();
    consistencyStore.groups = dummyGroupInconsistency;
    // make a copy of initial state

    // when
    await consistencyStore.refresh(dummyGroupInconsistency[0].name);

    // then
    expect(consistencyStore.groups.length).toEqual(1);
    expect(consistencyStore.groups).toEqual(dummyGroupInconsistency2);
  });
});
