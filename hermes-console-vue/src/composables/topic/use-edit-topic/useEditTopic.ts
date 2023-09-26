import { dispatchErrorNotification } from '@/utils/notification-utils';
import { editTopic as doEditTopic, fetchOwner } from '@/api/hermes-client';
import {
  initializeFullyFilledForm,
  useFormTopic,
  watchOwnerSearch,
} from '@/composables/topic/use-form-topic/useFormTopic';
import { ref, watch } from 'vue';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { TopicWithSchema } from '@/api/topic';
import type {
  UseEditTopic,
  UseEditTopicErrors,
} from '@/composables/topic/use-edit-topic/types';

export function useEditTopic(topic: TopicWithSchema): UseEditTopic {
  const notificationsStore = useNotificationsStore();

  const errors = ref<UseEditTopicErrors>({
    fetchOwnerSources: null,
    fetchOwners: null,
  });
  const { form, validators, dataSources } = useFormTopic();
  initializeFullyFilledForm(form, topic);
  watchOwnerSearch(form, dataSources, errors);

  watch(dataSources.ownerSources, (ownerSources) => {
    form.value.ownerSource = ownerSources.find(
      (ownerSource) => ownerSource.value.name === topic.owner.source,
    )?.value!!;
  });

  fetchOwner(topic.owner.id, topic.owner.source)
    .then((owner) => owner.data)
    .then(
      (owner) =>
        (dataSources.owners.value = [{ title: owner.name, value: owner.id }]),
    );

  const updatingTopic = ref(false);

  async function editTopic(): Promise<boolean> {
    updatingTopic.value = true;

    try {
      await doEditTopic(form.value);
      await notificationsStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.topic.edit.success', {
          topicName: form.value.name,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      await dispatchErrorNotification(
        e,
        notificationsStore,
        useGlobalI18n().t('notifications.topic.edit.failure'),
      );
    } finally {
      updatingTopic.value = false;
    }

    return false;
  }

  return {
    form,
    validators,
    dataSources,
    creatingOrUpdatingTopic: updatingTopic,
    errors,
    createOrUpdateTopic: editTopic,
  };
}
