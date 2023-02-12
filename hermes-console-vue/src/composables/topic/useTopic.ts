import type { TopicWithSchema } from "@/api/topic";
import type { Owner } from "@/api/owner";
import { ref } from "vue";
import { sendRequest } from "@/utils/send-request";

export function useTopic(topicName: string) {
  const topic = ref<TopicWithSchema | null>(null);
  const topicError = ref<any | null>(null);
  const owner = ref<Owner | null>(null);
  const ownerError = ref<any | null>(null);

  fetchTopic(topicName)
    .then(t => (topic.value = t))
    .catch(e => (topicError.value = e))
    .then(t => fetchTopicOwner(t.owner.id))
    .then(o => (owner.value = o))
    .catch(e => (ownerError.value = e));

  return {
    topic,
    owner,
    topicError,
    ownerError
  };
}

const fetchTopic = (topicName: string): Promise<TopicWithSchema> =>
  sendRequest(`http://localhost:3000/topics/${ topicName }`);

const fetchTopicOwner = (ownerId: string): Promise<Owner> =>
  sendRequest(`http://localhost:3000/owners/sources/Service Catalog/${ ownerId }`);
