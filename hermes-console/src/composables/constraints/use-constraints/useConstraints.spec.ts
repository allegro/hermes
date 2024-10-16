import { afterEach } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import {
  deleteSubscriptionConstraintHandler,
  deleteTopicConstraintHandler,
  fetchConstraintsErrorHandler,
  fetchConstraintsHandler,
  upsertSubscriptionConstraintHandler,
  upsertTopicConstraintHandler,
} from '@/mocks/handlers';
import { dummyConstraints } from '@/dummy/constraints';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useConstraints } from '@/composables/constraints/use-constraints/useConstraints';
import { waitFor } from '@testing-library/vue';

describe('useConstraints', () => {
  const server = setupServer(
    fetchConstraintsHandler({ constraints: dummyConstraints }),
  );

  const pinia = createTestingPinia({
    fakeApp: true,
  });

  beforeEach(() => {
    setActivePinia(pinia);
  });

  afterEach(() => {
    server.resetHandlers();
  });

  it('should fetch constraints names from Hermes backend', async () => {
    // given
    server.listen();

    // when
    const { topicConstraints, subscriptionConstraints, loading, error } =
      useConstraints();

    // then
    expect(loading.value).toBeTruthy();
    expect(error.value.fetchConstraints).toBeNull();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchConstraints).toBeNull();
      expect(topicConstraints.value?.['pl.group.Topic1'].consumersNumber).toBe(
        2,
      );
      expect(topicConstraints.value?.['pl.group.Topic1'].reason).toBe(
        'Some reason',
      );
      expect(
        subscriptionConstraints.value?.['pl.group.Topic$subscription2']
          .consumersNumber,
      ).toBe(8);
    });
  });

  it('should set error to true on workload endpoint failure', async () => {
    // given
    server.use(fetchConstraintsErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { error } = useConstraints();

    // then
    await waitFor(() => {
      expect(error.value.fetchConstraints).not.toBeNull();
    });
  });

  it('should dispatch notification on successful topic constraint upsert', async () => {
    // given
    server.use(
      upsertTopicConstraintHandler({
        statusCode: 200,
      }),
    );
    server.listen();
    const notificationsStore = notificationStoreSpy();

    // when
    const { upsertTopicConstraint } = useConstraints();
    const success = await upsertTopicConstraint('topic', {
      consumersNumber: 3,
    });

    // then
    expect(success).toBeTruthy();
    await waitFor(() => {
      expectNotificationDispatched(notificationsStore, {
        type: 'success',
        title: 'notifications.constraints.topic.created.success',
      });
    });
  });

  it('should dispatch notification on unsuccessful topic constraint upsert', async () => {
    // given
    server.use(
      upsertTopicConstraintHandler({
        statusCode: 500,
      }),
    );
    server.listen();
    const notificationsStore = notificationStoreSpy();

    // when
    const { upsertTopicConstraint } = useConstraints();
    const success = await upsertTopicConstraint('topic', {
      consumersNumber: 3,
    });

    // then
    expect(success).toBeFalsy();
    await waitFor(() => {
      expectNotificationDispatched(notificationsStore, {
        type: 'error',
        title: 'notifications.constraints.topic.created.failure',
      });
    });
  });

  it('should dispatch notification on successful subscription constraint upsert', async () => {
    // given
    server.use(
      upsertSubscriptionConstraintHandler({
        statusCode: 200,
      }),
    );
    server.listen();
    const notificationsStore = notificationStoreSpy();

    // when
    const { upsertSubscriptionConstraint } = useConstraints();
    const success = await upsertSubscriptionConstraint('topic$subscription', {
      consumersNumber: 3,
    });

    // then
    expect(success).toBeTruthy();
    await waitFor(() => {
      expectNotificationDispatched(notificationsStore, {
        type: 'success',
        title: 'notifications.constraints.subscription.created.success',
      });
    });
  });

  it('should dispatch notification on unsuccessful subscription constraint upsert', async () => {
    // given
    server.use(
      upsertSubscriptionConstraintHandler({
        statusCode: 500,
      }),
    );
    server.listen();
    const notificationsStore = notificationStoreSpy();

    // when
    const { upsertSubscriptionConstraint } = useConstraints();
    const success = await upsertSubscriptionConstraint('topic$subscription', {
      consumersNumber: 3,
    });

    // then
    expect(success).toBeFalsy();
    await waitFor(() => {
      expectNotificationDispatched(notificationsStore, {
        type: 'error',
        title: 'notifications.constraints.subscription.created.failure',
      });
    });
  });

  it('should dispatch notification on successful topic constraint delete', async () => {
    // given
    server.use(
      deleteTopicConstraintHandler({
        statusCode: 200,
        topicName: 'topic',
      }),
    );
    server.listen();
    const notificationsStore = notificationStoreSpy();

    // when
    const { deleteTopicConstraint } = useConstraints();
    const success = await deleteTopicConstraint('topic');

    // then
    expect(success).toBeTruthy();
    await waitFor(() => {
      expectNotificationDispatched(notificationsStore, {
        type: 'success',
        title: 'notifications.constraints.topic.deleted.success',
      });
    });
  });

  it('should dispatch notification on unsuccessful topic constraint delete', async () => {
    // given
    server.use(
      deleteTopicConstraintHandler({
        statusCode: 500,
        topicName: 'topic',
      }),
    );
    server.listen();
    const notificationsStore = notificationStoreSpy();

    // when
    const { deleteTopicConstraint } = useConstraints();
    const success = await deleteTopicConstraint('topic');

    // then
    expect(success).toBeFalsy();
    await waitFor(() => {
      expectNotificationDispatched(notificationsStore, {
        type: 'error',
        title: 'notifications.constraints.topic.deleted.failure',
      });
    });
  });

  it('should dispatch notification on successful subscription constraint delete', async () => {
    // given
    server.use(
      deleteSubscriptionConstraintHandler({
        statusCode: 200,
        topicName: 'topic',
        subscriptionName: 'subscription',
      }),
    );
    server.listen();
    const notificationsStore = notificationStoreSpy();

    // when
    const { deleteSubscriptionConstraint } = useConstraints();
    const success = await deleteSubscriptionConstraint('topic$subscription');

    // then
    expect(success).toBeTruthy();
    await waitFor(() => {
      expectNotificationDispatched(notificationsStore, {
        type: 'success',
        title: 'notifications.constraints.subscription.deleted.success',
      });
    });
  });

  it('should dispatch notification on unsuccessful subscription constraint delete', async () => {
    // given
    server.use(
      deleteSubscriptionConstraintHandler({
        statusCode: 500,
        topicName: 'topic',
        subscriptionName: 'subscription',
      }),
    );
    server.listen();
    const notificationsStore = notificationStoreSpy();

    // when
    const { deleteSubscriptionConstraint } = useConstraints();
    const success = await deleteSubscriptionConstraint('topic$subscription');

    // then
    expect(success).toBeFalsy();
    await waitFor(() => {
      expectNotificationDispatched(notificationsStore, {
        type: 'error',
        title: 'notifications.constraints.subscription.deleted.failure',
      });
    });
  });
});
