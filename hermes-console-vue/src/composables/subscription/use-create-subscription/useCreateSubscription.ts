import { AxiosError } from 'axios';
import { computed, ref, watch } from 'vue';
import {
  fetchOwnersSources,
  hermesClient,
  searchOwners,
} from '@/api/hermes-client';
import {
  getDataSources,
  useFormSubscription,
} from '@/composables/subscription/use-form-subscription/useFormSubscription';
import { parseFormToRequestBody } from '@/composables/subscription/use-create-subscription/form-mapper';
import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { AxiosResponse } from 'axios';
import type { CreateSubscriptionFormRequestBody } from '@/api/subscription';
import type {
  DataSources,
  SubscriptionForm,
  UseCreateSubscription,
  UseCreateSubscriptionErrors,
} from '@/composables/subscription/use-create-subscription/types';

export function useCreateSubscription(topic: string): UseCreateSubscription {
  const { loadedConfig } = useAppConfigStore();
  const errors = ref<UseCreateSubscriptionErrors>({
    fetchOwnerSources: null,
    fetchOwners: null,
  });
  const rawDataSources = getDataSources();
  const initialFormState = createInitialFormState(rawDataSources);
  const { form, validators } = useFormSubscription(initialFormState);
  const dataSources: DataSources = {
    ...rawDataSources,
    contentTypes: computed(() =>
      rawDataSources.contentTypes.filter(
        (contentType) =>
          !contentType.unsupportedDeliveryTypes.includes(
            form.value.deliveryType,
          ),
      ),
    ),
  };
  const searchTimeout = ref();
  watch(
    () => form.value.ownerSearch,
    async (searchingPhrase) => {
      const selectedOwnerSource = form.value.ownerSource;
      if (!selectedOwnerSource || !searchingPhrase) {
        return;
      }
      clearTimeout(searchTimeout.value);
      searchTimeout.value = setTimeout(async () => {
        try {
          dataSources.loadingOwners.value = true;
          dataSources.owners.value = (
            await searchOwners(selectedOwnerSource.name, searchingPhrase)
          ).data.map((source) => {
            return { title: source.name, value: source.id };
          });
        } catch (e) {
          errors.value.fetchOwners = e as Error;
        } finally {
          dataSources.loadingOwners.value = false;
        }
      }, 500);
    },
  );
  watch(
    () => form.value.deliveryType,
    () => {
      form.value.contentType = '';
    },
  );
  const validators = formValidators(form);

  const creatingSubscription = ref(false);

  async function createSubscription() {
    creatingSubscription.value = true;
    let requestBody: CreateSubscriptionFormRequestBody | null = null;

    try {
      requestBody = parseFormToRequestBody(topic, form.value);
    } catch (e) {
      const notificationsStore = useNotificationsStore();
      notificationsStore.dispatchNotification({
        title: 'Failed creating subscription',
        text: 'Error parsing form data',
        type: 'error',
      });
      creatingSubscription.value = false;
    }

    let response: AxiosResponse<void, any> | null;
    try {
      response = await hermesClient.createSubscription(topic, requestBody!!);
    } catch (e: any) {
      const notificationsStore = useNotificationsStore();
      const text =
        e instanceof AxiosError ? e.message : 'Unknown error occurred';
      notificationsStore.dispatchNotification({
        title: 'Failed creating subscription',
        text,
        type: 'error',
      });
    } finally {
      creatingSubscription.value = false;
    }
  }

  return {
    form,
    validators,
    dataSources,
    creatingSubscription,
    errors,
    createSubscription,
  };
}

function createInitialFormState(dataSources: DataSources): SubscriptionForm {
  const { loadedConfig } = useAppConfigStore();
  return {
    name: '',
    endpoint: '',
    description: '',
    ownerSource: null,
    owner: '',
    ownerSearch: '',
    contentType: '',
    deliveryType: loadedConfig.subscription.defaults.deliveryType,
    mode: dataSources.deliveryModes[0].value,
    subscriptionPolicy: {
      rateLimit: null,
      inflightMessageTTL:
        loadedConfig.subscription.defaults.subscriptionPolicy.messageTtl ||
        3600,
      retryBackoff: 1000,
      retryBackoffMultiplier: 1.0,
      sendingDelay: 0,
      requestTimeout:
        loadedConfig.subscription.defaults.subscriptionPolicy.requestTimeout ||
        1000,
      batchSize: null,
      batchTime: null,
      batchVolume: null,
    },
    retryOn4xx: false,
    messageDeliveryTrackingMode:
      dataSources.messageDeliveryTrackingModes[0].value,
    monitoringDetails: {
      severity: dataSources.monitoringSeverities[0].value,
      reaction: '',
    },
    deliverUsingHttp2: false,
    attachSubscriptionIdentityHeaders: false,
    deleteSubscriptionAutomatically: false,
    pathFilters: [],
    headerFilters: [],
  };
}
