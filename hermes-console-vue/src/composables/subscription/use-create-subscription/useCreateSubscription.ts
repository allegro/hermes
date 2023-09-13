import {
  createSubscription as doCreateSubscription,
  searchOwners,
} from '@/api/hermes-client';
import { parseFormToRequestBody } from '@/composables/subscription/use-form-subscription/form-mapper';
import { ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
import { useFormSubscription } from '@/composables/subscription/use-form-subscription/useFormSubscription';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { CreateSubscriptionFormRequestBody } from '@/api/subscription';
import type {
  DataSources,
  SubscriptionForm,
} from '@/composables/subscription/use-form-subscription/types';
import type { Ref } from 'vue';
import type {
  UseCreateSubscription,
  UseCreateSubscriptionErrors,
} from '@/composables/subscription/use-create-subscription/types';

export function useCreateSubscription(topic: string): UseCreateSubscription {
  const notificationsStore = useNotificationsStore();

  const errors = ref<UseCreateSubscriptionErrors>({
    fetchOwnerSources: null,
    fetchOwners: null,
  });
  const { form, validators, dataSources } = useFormSubscription();
  initializeForm(form, dataSources);
  watch(
    () => form.value.deliveryType,
    () => {
      form.value.contentType = '';
    },
  );
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

  const creatingSubscription = ref(false);

  async function createSubscription(): Promise<boolean> {
    creatingSubscription.value = true;
    let requestBody: CreateSubscriptionFormRequestBody | null = null;

    try {
      requestBody = parseFormToRequestBody(topic, form.value);
    } catch (e) {
      notificationsStore.dispatchNotification({
        title: useGlobalI18n().t('notifications.subscription.create.failure'),
        text: useGlobalI18n().t('notifications.form.parseError'),
        type: 'error',
      });
      creatingSubscription.value = false;
      return false;
    }

    try {
      await doCreateSubscription(topic, requestBody!!);
      notificationsStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.subscription.create.success', {
          subscriptionName: form.value.name,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      const text = e.response?.data?.message
        ? e.response.data.message
        : 'Unknown error occurred';
      notificationsStore.dispatchNotification({
        title: useGlobalI18n().t('notifications.subscription.create.failure'),
        text,
        type: 'error',
      });
    } finally {
      creatingSubscription.value = false;
    }

    return false;
  }

  return {
    form,
    validators,
    dataSources,
    creatingOrUpdatingSubscription: creatingSubscription,
    errors,
    createOrUpdateSubscription: createSubscription,
  };
}

function initializeForm(
  form: Ref<SubscriptionForm>,
  dataSources: DataSources,
): void {
  const { loadedConfig } = storeToRefs(useAppConfigStore());
  form.value = {
    name: '',
    endpoint: '',
    description: '',
    ownerSource: null,
    owner: '',
    ownerSearch: '',
    contentType: '',
    deliveryType: loadedConfig.value.subscription.defaults.deliveryType,
    mode: dataSources.deliveryModes[0].value,
    subscriptionPolicy: {
      rateLimit: null,
      inflightMessageTTL:
        loadedConfig.value.subscription.defaults.subscriptionPolicy
          .messageTtl || 3600,
      retryBackoff: 1000,
      retryBackoffMultiplier: 1.0,
      sendingDelay: 0,
      requestTimeout:
        loadedConfig.value.subscription.defaults.subscriptionPolicy
          .requestTimeout || 1000,
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
