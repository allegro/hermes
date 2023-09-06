import { AxiosError } from 'axios';
import { DeliveryType } from '@/api/subscription';
import { fetchOwner, hermesClient, searchOwners } from '@/api/hermes-client';
import { v4 as generateUUID } from 'uuid';
import { parseFormToRequestBody } from '@/composables/subscription/use-create-subscription/form-mapper';
import { ref, watch } from 'vue';
import { useFormSubscription } from '@/composables/subscription/use-form-subscription/useFormSubscription';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type {
  CreateSubscriptionFormRequestBody,
  MessageFilterSpecification,
  Subscription,
} from '@/api/subscription';
import type { HeaderFilter } from '@/views/subscription/subscription-form/subscription-header-filters/types';
import type { PathFilter } from '@/views/subscription/subscription-form/subscription-basic-filters/types';
import type { Ref } from 'vue';
import type { SubscriptionForm } from '@/composables/subscription/use-form-subscription/types';
import type { UseCreateSubscriptionErrors } from '@/composables/subscription/use-create-subscription/types';
import type { UseEditSubscription } from '@/composables/subscription/use-edit-subscription/types';

export function useEditSubscription(
  topic: string,
  subscription: Subscription,
): UseEditSubscription {
  const notificationsStore = useNotificationsStore();

  const errors = ref<UseCreateSubscriptionErrors>({
    fetchOwnerSources: null,
    fetchOwners: null,
  });
  const { form, validators, dataSources } = useFormSubscription();
  initializeForm(form, subscription);
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

  watch(dataSources.ownerSources, (ownerSources) => {
    form.value.ownerSource = ownerSources.find(
      (ownerSource) => ownerSource.value.name === subscription.owner.source,
    )?.value!!;
  });

  fetchOwner(subscription.owner.id)
    .then((owner) => owner.data)
    .then(
      (owner) =>
        (dataSources.owners.value = [{ title: owner.name, value: owner.id }]),
    );

  const updatingSubscription = ref(false);

  async function editSubscription(): Promise<boolean> {
    updatingSubscription.value = true;
    let requestBody: CreateSubscriptionFormRequestBody | null = null;

    try {
      requestBody = parseFormToRequestBody(topic, form.value);
    } catch (e) {
      notificationsStore.dispatchNotification({
        title: 'Failed creating subscription',
        text: 'Error parsing form data',
        type: 'error',
      });
      updatingSubscription.value = false;
      return false;
    }

    try {
      await hermesClient.editSubscription(
        topic,
        subscription.name,
        requestBody!!,
      );
      notificationsStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.subscription.edit.success', {
          subscriptionName: form.value.name,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      const text =
        e instanceof AxiosError ? e.message : 'Unknown error occurred';
      notificationsStore.dispatchNotification({
        title: useGlobalI18n().t('notifications.subscription.edit.failure'),
        text,
        type: 'error',
      });
    } finally {
      updatingSubscription.value = false;
    }

    return false;
  }

  return {
    form,
    validators,
    dataSources,
    creatingOrUpdatingSubscription: updatingSubscription,
    errors,
    createOrUpdateSubscription: editSubscription,
  };
}

export function initializeForm(
  form: Ref<SubscriptionForm>,
  subscription: Subscription,
): void {
  form.value = {
    name: subscription.name,
    endpoint: subscription.endpoint,
    description: subscription.description,
    ownerSource: null,
    owner: subscription.owner.id,
    ownerSearch: '',
    contentType: subscription.contentType,
    deliveryType: subscription.deliveryType,
    mode: subscription.mode,
    subscriptionPolicy: {
      rateLimit:
        subscription.deliveryType === DeliveryType.SERIAL
          ? subscription.subscriptionPolicy.rate
          : null,
      inflightMessageTTL: subscription.subscriptionPolicy.messageTtl,
      retryBackoff: subscription.subscriptionPolicy.messageBackoff,
      retryBackoffMultiplier:
        subscription.deliveryType === DeliveryType.SERIAL
          ? subscription.subscriptionPolicy.backoffMultiplier
          : 1.0,
      sendingDelay:
        subscription.deliveryType === DeliveryType.SERIAL
          ? subscription.subscriptionPolicy.sendingDelay
          : 0,
      requestTimeout: subscription.subscriptionPolicy.requestTimeout,
      batchSize:
        subscription.deliveryType === DeliveryType.BATCH
          ? subscription.subscriptionPolicy.batchSize
          : null,
      batchTime:
        subscription.deliveryType === DeliveryType.BATCH
          ? subscription.subscriptionPolicy.batchTime
          : null,
      batchVolume:
        subscription.deliveryType === DeliveryType.BATCH
          ? subscription.subscriptionPolicy.batchVolume
          : null,
    },
    retryOn4xx: subscription.subscriptionPolicy.retryClientErrors,
    messageDeliveryTrackingMode: subscription.trackingMode,
    monitoringDetails: {
      severity: subscription.monitoringDetails.severity,
      reaction: subscription.monitoringDetails.reaction,
    },
    deliverUsingHttp2: subscription.http2Enabled,
    attachSubscriptionIdentityHeaders:
      subscription.subscriptionIdentityHeadersEnabled,
    deleteSubscriptionAutomatically: subscription.autoDeleteWithTopicEnabled,
    pathFilters: mapToPathFilter(subscription.filters),
    headerFilters: mapToHeaderFilter(subscription.filters),
  };
}

function mapToHeaderFilter(
  filters: MessageFilterSpecification,
): HeaderFilter[] {
  return filters
    .filter((filter: MessageFilterSpecification) => filter.type === 'header')
    .map((filter: MessageFilterSpecification) => {
      return {
        id: generateUUID(),
        headerName: filter.header,
        matcher: filter.matcher,
      };
    });
}

function mapToPathFilter(filters: MessageFilterSpecification): PathFilter[] {
  return filters
    .filter((filter: MessageFilterSpecification) => filter.type !== 'header')
    .map((filter: MessageFilterSpecification) => {
      return {
        id: generateUUID(),
        path: filter.path,
        matcher: filter.matcher,
        matchingStrategy: filter.matchingStrategy,
      };
    });
}
