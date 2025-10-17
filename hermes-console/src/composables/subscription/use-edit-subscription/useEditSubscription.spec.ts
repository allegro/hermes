import { afterEach, expect } from 'vitest';
import { createTestingPiniaWithState } from '@/dummy/store';
import {
  dummyInitializedEditSubscriptionForm,
  dummyOwnerSources,
} from '@/dummy/subscription-form';
import { dummySubscription } from '@/dummy/subscription';
import { dummyTopic } from '@/dummy/topic';
import {
  editSubscriptionErrorHandler,
  editSubscriptionHandler,
  fetchOwnerHandler,
  fetchOwnerSourcesHandler,
  fetchTopicErrorHandler,
  fetchTopicHandler,
} from '@/mocks/handlers';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import { parseFormToRequestBody } from '@/composables/subscription/use-form-subscription/form-mapper';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useEditSubscription } from '@/composables/subscription/use-edit-subscription/useEditSubscription';
import { waitFor } from '@testing-library/vue';

vi.mock('@/composables/subscription/use-form-subscription/form-mapper');

describe('useEditSubscription', () => {
  const server = setupServer(
    fetchOwnerSourcesHandler(dummyOwnerSources),
    fetchOwnerHandler({}),
    fetchTopicHandler({
      topic: dummyTopic,
    }),
  );

  beforeEach(() => {
    setActivePinia(createTestingPiniaWithState());
  });

  afterEach(() => {
    server.resetHandlers();
  });

  it('should return initialized form', async () => {
    // given
    server.listen();

    // when
    const { form } = useEditSubscription(
      dummySubscription.topicName,
      dummySubscription,
    );

    // helper to normalize both actual and expected forms
    const normalizeForms = (data: any) => ({
      ...data,
      pathFilters: data.pathFilters.map(({ id, type, ...rest }: any) => {
        void id;
        void type;
        return rest;
      }),
      headerFilters: data.headerFilters.map(({ id, type, ...rest }: any) => {
        void id;
        void type;
        return rest;
      }),
      headers: data.headers.map(({ id, ...rest }: any) => {
        void id;
        return rest;
      }),
    });

    // then
    const normalizedActual = normalizeForms(form.value);
    const normalizedExpected = normalizeForms(
      dummyInitializedEditSubscriptionForm,
    );
    expect(JSON.stringify(normalizedActual)).toMatchObject(
      JSON.stringify(normalizedExpected),
    );
  });

  it('should dispatch notification about subscription edit parse error', async () => {
    //given
    server.use(
      editSubscriptionHandler(
        dummySubscription.topicName,
        dummySubscription.name,
      ),
    );
    vi.mocked(parseFormToRequestBody).mockImplementationOnce(() => {
      throw new Error('error');
    });
    server.listen();
    const notificationStore = notificationStoreSpy();
    const { createOrUpdateSubscription } = useEditSubscription(
      dummySubscription.topicName,
      dummySubscription,
    );

    // when
    await createOrUpdateSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        text: 'notifications.form.parseError',
        title: 'notifications.subscription.edit.failure',
        type: 'error',
      });
    });
  });

  it('should dispatch notification about subscription edit request error', async () => {
    //given
    // @ts-ignore
    vi.mocked(parseFormToRequestBody).mockReturnValueOnce(null);
    server.use(
      editSubscriptionErrorHandler(
        dummySubscription.topicName,
        dummySubscription.name,
        500,
      ),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();
    const { createOrUpdateSubscription } = useEditSubscription(
      dummySubscription.topicName,
      dummySubscription,
    );

    // when
    await createOrUpdateSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        title: 'notifications.subscription.edit.failure',
        type: 'error',
      });
    });
  });

  it('should dispatch notification about subscription edit success', async () => {
    //given
    // @ts-ignore
    vi.mocked(parseFormToRequestBody).mockReturnValueOnce(null);
    server.use(
      editSubscriptionHandler(
        dummySubscription.topicName,
        dummySubscription.name,
      ),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();
    const { createOrUpdateSubscription } = useEditSubscription(
      dummySubscription.topicName,
      dummySubscription,
    );

    // when
    await createOrUpdateSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        text: 'notifications.subscription.edit.success',
        type: 'success',
      });
    });
  });

  it('should dispatch notification about fetchTopicContentType error', async () => {
    //given
    // @ts-ignore
    vi.mocked(parseFormToRequestBody).mockReturnValueOnce(null);
    server.use(
      editSubscriptionHandler(
        dummySubscription.topicName,
        dummySubscription.name,
      ),
      fetchTopicErrorHandler({
        topicName: dummySubscription.topicName,
        errorCode: 500,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();
    const { createOrUpdateSubscription } = useEditSubscription(
      dummySubscription.topicName,
      dummySubscription,
    );

    // when
    await createOrUpdateSubscription();

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        text: 'notifications.form.fetchTopicContentTypeError',
        type: 'error',
      });
    });
  });
});
