import { dispatchErrorNotification } from '@/utils/notification-utils';
import { createSubscription as doCreateSubscription } from '@/api/hermes-client';
import { fetchContentType } from '@/composables/topic/use-topic/useTopic';
import { parseFormToRequestBody } from '@/composables/subscription/use-form-subscription/form-mapper';
import { ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
import {
  useFormSubscription,
  watchOwnerSearch,
} from '@/composables/subscription/use-form-subscription/useFormSubscription';
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
  watchOwnerSearch(form, dataSources, errors);

  const creatingSubscription = ref(false);

  async function createSubscription(): Promise<boolean> {
    creatingSubscription.value = true;
    let requestBody: CreateSubscriptionFormRequestBody | null = null;

    const topicContentType = await fetchContentType(topic);

    if (topicContentType.error) {
      await notificationsStore.dispatchNotification({
        title: useGlobalI18n().t('notifications.subscription.create.failure'),
        text: useGlobalI18n().t(
          'notifications.form.fetchTopicContentTypeError',
        ),
        type: 'error',
      });
      return false;
    }

    try {
      requestBody = parseFormToRequestBody(
        topic,
        form.value,
        topicContentType.contentType!!,
      );
    } catch (e) {
      await notificationsStore.dispatchNotification({
        title: useGlobalI18n().t('notifications.subscription.create.failure'),
        text: useGlobalI18n().t('notifications.form.parseError'),
        type: 'error',
      });
      creatingSubscription.value = false;
      return false;
    }

    try {
      await doCreateSubscription(topic, requestBody!!);
      await notificationsStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.subscription.create.success', {
          subscriptionName: form.value.name,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      await dispatchErrorNotification(
        e,
        notificationsStore,
        useGlobalI18n().t('notifications.subscription.create.failure'),
      );
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
      inflightMessagesCount: null,
      retryBackoff: 1000,
      retryBackoffMultiplier: 1.0,
      backoffMaxIntervalInSec: 600,
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
    endpointAddressResolverMetadata: form.value.endpointAddressResolverMetadata,
  };
}
