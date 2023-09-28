import type {
  FormOfflineStorage,
  FormRetentionTime,
  TopicForm,
} from '@/composables/topic/use-form-topic/types';
import type { OwnerSource } from '@/api/owner';

type TopicFormBody = {
  owner: { source: string | undefined; id: string };
  schema?: string;
  auth: {
    publishers: string[];
    unauthenticatedAccessEnabled: boolean;
    enabled: boolean;
  };
  ack: string;
  description: string;
  ownerSource?: OwnerSource | null;
  ownerSearch?: string;
  retentionTime: FormRetentionTime;
  trackingEnabled: boolean;
  offlineStorage: FormOfflineStorage;
  subscribingRestricted: boolean;
  name: string;
  maxMessageSize: number;
  contentType: string;
};

export function parseTopicForm(
  topicForm: TopicForm,
  group: string | null,
): TopicFormBody {
  delete topicForm.ownerSearch;
  delete topicForm.offlineStorage.retentionTime.retentionUnit;
  delete topicForm.retentionTime.infinite;
  if (topicForm.contentType !== 'AVRO') {
    delete topicForm.schema;
  }
  if (group) {
    topicForm.name = `${group}.${topicForm.name}`;
  }
  const parsedRequestBody = {
    ...topicForm,
    owner: { source: topicForm.ownerSource?.name, id: topicForm.owner },
    auth: {
      ...topicForm.auth,
      publishers: topicForm.auth.publishers.split(','),
    },
  };
  delete parsedRequestBody.ownerSource;
  return parsedRequestBody;
}
