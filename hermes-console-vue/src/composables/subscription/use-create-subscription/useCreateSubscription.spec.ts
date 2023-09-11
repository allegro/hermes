import { afterEach, expect } from 'vitest';
import {
  createSubscriptionErrorHandler,
  createSubscriptionHandler,
  fetchOwnerSourcesHandler,
} from '@/mocks/handlers';
import { createTestingPiniaWithState } from '@/dummy/store';
import {
  dummyInitializedSubscriptionForm,
  dummyOwnerSources,
} from '@/dummy/form';
import { dummySubscription } from '@/dummy/subscription';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import { parseFormToRequestBody } from '@/composables/subscription/use-form-subscription/form-mapper';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useCreateSubscription } from '@/composables/subscription/use-create-subscription/useCreateSubscription';
import { waitFor } from '@testing-library/vue';

vi.mock('@/composables/subscription/use-form-subscription/form-mapper');

describe('useCreateSubscription', () => {
  const server = setupServer(fetchOwnerSourcesHandler(dummyOwnerSources));

  beforeEach(() => {
    setActivePinia(createTestingPiniaWithState());
  });

  afterEach(() => {
    server.resetHandlers();
  });

  it('should return initialized form', async () => {
    //given
    server.listen();

    // when
    const { form } = useCreateSubscription(dummySubscription.topicName);

    // then
    expect(JSON.stringify(form.value)).toMatchObject(
      JSON.stringify(dummyInitializedSubscriptionForm),
    );
  });

  it('should dispatch notification about subscription creation parse error', async () => {
    //given
    server.use(createSubscriptionHandler(dummySubscription.topicName));
    vi.mocked(parseFormToRequestBody).mockImplementationOnce(() => {
      throw new Error('error');
    });
    server.listen();
    const notificationStore = notificationStoreSpy();
    const { createOrUpdateSubscription } = useCreateSubscription(
      dummySubscription.topicName,
    );

    // when
    await createOrUpdateSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        text: 'notifications.form.parseError',
        title: 'notifications.subscription.create.failure',
        type: 'error',
      });
    });
  });

  it('should dispatch notification about subscription creation request error', async () => {
    //given
    // @ts-ignore
    vi.mocked(parseFormToRequestBody).mockReturnValueOnce(null);
    server.use(
      createSubscriptionErrorHandler(dummySubscription.topicName, 500),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();
    const { createOrUpdateSubscription } = useCreateSubscription(
      dummySubscription.topicName,
    );

    // when
    await createOrUpdateSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        title: 'notifications.subscription.create.failure',
        type: 'error',
      });
    });
  });

  it('should dispatch notification about subscription creation success', async () => {
    //given
    // @ts-ignore
    vi.mocked(parseFormToRequestBody).mockReturnValueOnce(null);
    server.use(createSubscriptionHandler(dummySubscription.topicName));
    server.listen();
    const notificationStore = notificationStoreSpy();
    const { createOrUpdateSubscription } = useCreateSubscription(
      dummySubscription.topicName,
    );

    // when
    await createOrUpdateSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        text: 'notifications.subscription.create.success',
        type: 'success',
      });
    });
  });
});
