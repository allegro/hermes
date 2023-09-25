import { fetchOwner } from '@/api/hermes-client';
import { initializeFullyFilledForm } from '@/composables/topic/use-form-topic/useFormTopic';
import { topicName } from '@/utils/topic-utils/topic-utils';
import type {
  DataSources,
  TopicForm,
} from '@/composables/topic/use-form-topic/types';
import type { Ref } from 'vue';
export interface UseImportTopic {
  importFormData: (
    importedFile: Ref<any>,
    form: Ref<TopicForm>,
    dataSources: DataSources,
  ) => void;
}

export function useImportTopic(): UseImportTopic {
  function importFormData(
    importedFile: Ref<any>,
    form: Ref<TopicForm>,
    dataSources: DataSources,
  ) {
    if (importedFile.value) {
      const reader = new FileReader();

      reader.readAsText(importedFile.value[0]);

      reader.onload = function () {
        const topic = JSON.parse(<string>reader.result);
        initializeFullyFilledForm(form, topic);
        form.value.name = topicName(form.value.name);
        form.value.ownerSource = dataSources.ownerSources.value.find(
          (ownerSource: { value: { name: any } }) =>
            ownerSource.value.name === topic.owner.source,
        )?.value!!;
        fetchOwner(topic.owner.id, topic.owner.source)
          .then((owner) => owner.data)
          .then(
            (owner) =>
              (dataSources.owners.value = [
                { title: owner.name, value: owner.id },
              ]),
          );
      };
    }
  }

  return {
    importFormData,
  };
}
