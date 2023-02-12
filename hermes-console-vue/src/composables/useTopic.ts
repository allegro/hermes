import { useFetch } from "@/composables/useFetch";
import type { TopicWithSchema } from '@/api/topic';
import type { Owner } from "@/api/owner";
import { ref, watch, } from "vue";

export function useTopic(topicName: string) {
  const { data: topic, error: topicError } = useFetch<TopicWithSchema>(
    `http://localhost:3000/topics/${topicName}`
  );
  const owner = ref<Owner | null>(null);
  const ownerError = ref<Error | null>(null);

  watch(topic, async () => {
    if (topic.value) {
      const ownerData = useFetch<Owner>(`http://localhost:3000/owners/sources/Service Catalog/${topic.value.owner.id}`);
      owner.value = ownerData.data.value;
      ownerError.value = ownerData.error.value;
      console.warn(ownerData.error.value)
    }
  });

  return {
    topic,
    owner,
    topicError,
    ownerError,
  };
}
