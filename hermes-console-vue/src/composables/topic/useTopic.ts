import { ref } from 'vue';
import { sendRequest } from '@/utils/send-request';
import type { Owner } from '@/api/owner';
import type { TopicWithSchema } from '@/api/topic';

export function useTopic(topicName: string) {
  const topic = ref<TopicWithSchema | null>(null);
  const topicError = ref<any | null>(null);
  const owner = ref<Owner | null>(null);
  const ownerError = ref<any | null>(null);
  const subscriptions = ref<string[] | null>(null);
  const subscriptionsError = ref<any | null>(null);

  fetchTopic(topicName)
    .then((data) => (topic.value = data))
    .catch((e) => (topicError.value = e))
    .then((data) => fetchTopicOwner(data.owner.id))
    .then((o) => (owner.value = o))
    .catch((e) => (ownerError.value = e))
    .then(() => fetchTopicSubscriptions(topicName))
    .then((data) => (subscriptions.value = data))
    .catch((e) => (subscriptionsError.value = e));

  return {
    topic,
    owner,
    subscriptions,
    topicError,
    ownerError,
    subscriptionsError,
  };
}

const fetchTopic = (topicName: string): Promise<TopicWithSchema> =>
  sendRequest(`http://localhost:3000/topics/${topicName}`);

const fetchTopicOwner = (ownerId: string): Promise<Owner> =>
  sendRequest(
    `http://localhost:3000/owners/sources/Service Catalog/${ownerId}`,
  );

const fetchTopicSubscriptions = (topicName: string): Promise<string[]> =>
  sendRequest(`http://localhost:3000/topics/${topicName}/subscriptions`);
