import { dispatchErrorNotification } from '@/utils/notification-utils';
import { createTopic as doCreateTopic } from '@/api/hermes-client';
import { ref } from 'vue';
import { storeToRefs } from 'pinia';
import { topicName } from '@/utils/topic-utils/topic-utils';
import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
import {
  useFormTopic,
  watchOwnerSearch,
} from '@/composables/topic/use-form-topic/useFormTopic';
import { useGlobalI18n } from '@/i18n';
import { useNotificationsStore } from '@/store/app-notifications/useAppNotifications';
import type { Ref } from 'vue';
import type { TopicForm } from '@/composables/topic/use-form-topic/types';
import type {
  UseCreateTopic,
  UseCreateTopicErrors,
} from '@/composables/topic/use-create-topic/types';

export const defaultMaxMessageSize = 10240;

export function useCreateTopic(group: string): UseCreateTopic {
  const notificationsStore = useNotificationsStore();

  const errors = ref<UseCreateTopicErrors>({
    fetchOwnerSources: null,
    fetchOwners: null,
  });
  const { form, validators, dataSources } = useFormTopic();
  initializeForm(form);
  watchOwnerSearch(form, dataSources, errors);

  const creatingTopic = ref(false);

  async function createTopic(): Promise<boolean> {
    creatingTopic.value = true;

    try {
      await doCreateTopic(form.value, group);
      await notificationsStore.dispatchNotification({
        text: useGlobalI18n().t('notifications.topic.create.success', {
          topicName: form.value.name,
        }),
        type: 'success',
      });
      return true;
    } catch (e: any) {
      await dispatchErrorNotification(
        e,
        notificationsStore,
        useGlobalI18n().t('notifications.topic.create.failure'),
      );
      form.value.name = topicName(form.value.name);
    } finally {
      creatingTopic.value = false;
    }

    return false;
  }

  return {
    form,
    validators,
    dataSources,
    creatingOrUpdatingTopic: creatingTopic,
    errors,
    createOrUpdateTopic: createTopic,
  };
}

function initializeForm(form: Ref<TopicForm>): void {
  const { loadedConfig } = storeToRefs(useAppConfigStore());
  form.value = {
    name: '',
    description: '',
    ownerSource: null,
    owner: '',
    ownerSearch: '',
    auth: {
      enabled: false,
      unauthenticatedAccessEnabled: false,
      publishers: '',
    },
    subscribingRestricted: false,
    retentionTime: {
      retentionUnit:
        loadedConfig.value.topic.defaults.retentionTime.retentionUnit,
      infinite: false,
      duration: loadedConfig.value.topic.defaults.retentionTime.duration,
    },
    offlineStorage: {
      enabled: loadedConfig.value.topic.defaults.offlineStorage.enabled,
      retentionTime: {
        retentionUnit:
          loadedConfig.value.topic.defaults.offlineStorage.retentionTime
            .retentionUnit,
        infinite: false,
        duration:
          loadedConfig.value.topic.defaults.offlineStorage.retentionTime
            .duration,
      },
    },
    trackingEnabled: false,
    contentType: loadedConfig.value.topic.defaults.contentType,
    maxMessageSize: defaultMaxMessageSize,
    ack: '',
    schema: '',
  };
}
