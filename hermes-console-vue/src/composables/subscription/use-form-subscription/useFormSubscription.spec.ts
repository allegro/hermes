import { afterEach, expect } from 'vitest';
import { createTestingPiniaWithState } from '@/dummy/store';
import {
  dummyContentTypes,
  dummyDeliveryModes,
  dummyDeliveryTypes,
  dummyMessageDeliveryTrackingModes,
  dummyMonitoringSeverities,
  dummyOwnerSources,
  dummySubscriptionForm,
  dummySubscriptionFormValidator,
} from '@/dummy/form';
import { fetchOwnerSourcesHandler } from '@/mocks/handlers';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useFormSubscription } from '@/composables/subscription/use-form-subscription/useFormSubscription';
import { waitFor } from '@testing-library/vue';

describe('useFormSubscription', () => {
  const server = setupServer(fetchOwnerSourcesHandler(dummyOwnerSources));

  beforeEach(() => {
    setActivePinia(createTestingPiniaWithState());
  });

  afterEach(() => {
    server.resetHandlers();
  });

  it('should return form and validators', async () => {
    //given
    server.listen();

    // when
    const { form, validators } = useFormSubscription();

    // then
    expect(JSON.stringify(form.value)).toMatchObject(
      JSON.stringify(dummySubscriptionForm),
    );
    expect(JSON.stringify(validators)).toMatchObject(
      JSON.stringify(dummySubscriptionFormValidator),
    );
  });

  it('should return dataSources', async () => {
    //given
    server.listen();

    // when
    const { dataSources } = useFormSubscription();

    // then
    await waitFor(() => {
      expect(JSON.stringify(dataSources.deliveryTypes)).toMatchObject(
        JSON.stringify(dummyDeliveryTypes),
      );
      expect(JSON.stringify(dataSources.contentTypes.value)).toMatchObject(
        JSON.stringify(dummyContentTypes),
      );
      expect(JSON.stringify(dataSources.deliveryModes)).toMatchObject(
        JSON.stringify(dummyDeliveryModes),
      );
      expect(JSON.stringify(dataSources.monitoringSeverities)).toMatchObject(
        JSON.stringify(dummyMonitoringSeverities),
      );
      expect(
        JSON.stringify(dataSources.messageDeliveryTrackingModes),
      ).toMatchObject(JSON.stringify(dummyMessageDeliveryTrackingModes));
      expect(JSON.stringify(dataSources.ownerSources.value)).toMatchObject(
        JSON.stringify(
          dummyOwnerSources
            .filter((source) => !source.deprecated)
            .map((source) => {
              return { title: source.name, value: source };
            }),
        ),
      );
    });
  });
});
