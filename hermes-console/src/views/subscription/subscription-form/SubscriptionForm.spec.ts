import { afterEach } from 'vitest';
import { createTestingPiniaWithState } from '@/dummy/store';
import { dummyOwnerSources } from '@/dummy/subscription-form';
import { dummySubscription } from '@/dummy/subscription';
import { expect } from 'vitest';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
  render,
} from '@/utils/test-utils';
import { fetchOwnerHandler, fetchOwnerSourcesHandler } from '@/mocks/handlers';
import { setupServer } from 'msw/node';
import { waitFor } from '@testing-library/vue';
import SubscriptionForm from '@/views/subscription/subscription-form/SubscriptionForm.vue';
import userEvent from '@testing-library/user-event';

describe('SubscriptionForm', () => {
  const server = setupServer(fetchOwnerSourcesHandler(dummyOwnerSources));

  afterEach(() => {
    server.resetHandlers();
  });

  const props = {
    topic: dummySubscription.topicName,
    subscription: null,
    operation: 'add',
    modelValue: true,
  };

  it('renders properly', () => {
    // given
    server.listen();

    // when
    const { getByText, getAllByText, queryByText } = render(SubscriptionForm, {
      testPinia: createTestingPiniaWithState(),
      props,
    });

    // then
    expect(getAllByText('subscriptionForm.fields.name.label')[0]).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.endpoint.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.description.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.ownerSource.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.owner.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.deliveryType.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.contentType.label')[0],
    ).toBeVisible();
    expect(getAllByText('subscriptionForm.fields.mode.label')[0]).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.rateLimit.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.requestTimeout.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.sendingDelay.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.inflightMessageTTL.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.retryOn4xx.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.retryBackoff.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.retryBackoffMultiplier.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.monitoringReaction.label')[0],
    ).toBeVisible();
    expect(
      getAllByText('subscriptionForm.fields.deliverUsingHttp2.label')[0],
    ).toBeVisible();
    expect(
      getAllByText(
        'subscriptionForm.fields.attachSubscriptionIdentityHeaders.label',
      )[0],
    ).toBeVisible();
    expect(
      getAllByText(
        'subscriptionForm.fields.deleteSubscriptionAutomatically.label',
      )[0],
    ).toBeVisible();
    expect(
      queryByText('subscriptionForm.actions.update'),
    ).not.toBeInTheDocument();

    expect(
      getByText('subscriptionForm.actions.create').closest('button'),
    ).toBeEnabled();
    expect(
      getByText('subscriptionForm.actions.cancel').closest('button'),
    ).toBeEnabled();
  });

  it('should render in edit mode', () => {
    // given
    server.use(fetchOwnerHandler({}));
    server.listen();

    // when
    const { getByText, queryByText } = render(SubscriptionForm, {
      testPinia: createTestingPiniaWithState(),
      props: {
        ...props,
        operation: 'edit',
        subscription: dummySubscription,
      },
    });

    // then
    expect(
      queryByText('subscriptionForm.actions.create'),
    ).not.toBeInTheDocument();

    expect(
      getByText('subscriptionForm.actions.update').closest('button'),
    ).toBeEnabled();
    expect(
      getByText('subscriptionForm.actions.cancel').closest('button'),
    ).toBeEnabled();
  });

  it('should dispatch notification about validation error', async () => {
    // given
    server.listen();
    const { getByText } = render(SubscriptionForm, {
      testPinia: createTestingPiniaWithState(),
      props,
    });
    const notificationStore = notificationStoreSpy();

    // when
    await userEvent.click(getByText('subscriptionForm.actions.create'));

    // then
    await waitFor(() => {
      expectNotificationDispatched(notificationStore, {
        type: 'error',
        title: 'notifications.form.validationError',
        text: '',
      });
    });
  });
});
