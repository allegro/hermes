import { dispatchErrorNotification } from '@/utils/notification-utils';
import {
  editSubscription as doEditSubscription,
  fetchOwner,
} from '@/api/hermes-client';
import {
  initializeFullyFilledForm,
  watchOwnerSearch,
} from '@/composables/subscription/use-form-subscription/useFormSubscription';
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
  initializeFullyFilledForm(form, subscription);

  watchOwnerSearch(form, dataSources, errors);

  watch(dataSources.ownerSources, (ownerSources) => {
    form.value.ownerSource = ownerSources.find(
      (ownerSource) => ownerSource.value.name === subscription.owner.source,
    )?.value!!;
  });

  fetchOwner(subscription.owner.id, subscription.owner.source)
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
      await notificationsStore.dispatchNotification({
        title: useGlobalI18n().t('notifications.subscription.edit.failure'),
        text: useGlobalI18n().t('notifications.form.parseError'),
        type: 'error',
      });
      updatingSubscription.value = false;
      return false;
    }

    try {
      await doEditSubscription(topic, subscription.name, requestBody!!);
      await notificationsStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.subscription.edit.success', {
          subscriptionName: form.value.name,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      await dispatchErrorNotification(
        e,
        notificationsStore,
        useGlobalI18n().t('notifications.subscription.edit.failure'),
      );
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
