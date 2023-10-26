import type {
  DataSources,
  FormValidators,
  TopicForm,
} from '@/composables/topic/use-form-topic/types';
import type { Ref } from 'vue';

export interface UseCreateTopic {
  form: Ref<TopicForm>;
  validators: FormValidators;
  dataSources: DataSources;
  creatingOrUpdatingTopic: Ref<boolean>;
  errors: Ref<UseCreateTopicErrors>;
  createOrUpdateTopic: () => Promise<boolean>;
}

export interface UseCreateTopicErrors {
  fetchOwnerSources: Error | null;
  fetchOwners: Error | null;
}
