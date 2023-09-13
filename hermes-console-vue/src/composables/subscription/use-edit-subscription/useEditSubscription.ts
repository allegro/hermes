import {
  editSubscription as doEditSubscription,
  fetchOwner,
  searchOwners,
} from '@/api/hermes-client';
import { initializeFulfilledForm } from '@/composables/subscription/use-form-subscription/useFormSubscription';
import { parseFormToRequestBody } from '@/composables/subscription/use-form-subscription/form-mapper';
import { ref, watch } from 'vue';
import { useFormSubscription } from '@/composables/subscription/use-form-subscription/useFormSubscription';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type {
  CreateSubscriptionFormRequestBody,
  Subscription,
} from '@/api/subscription';
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
  initializeFulfilledForm(form, subscription);
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
        title: useGlobalI18n().t('notifications.subscription.edit.failure'),
        text: useGlobalI18n().t('notifications.form.parseError'),
        type: 'error',
      });
      updatingSubscription.value = false;
      return false;
    }

    try {
      await doEditSubscription(topic, subscription.name, requestBody!!);
      notificationsStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.subscription.edit.success', {
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
