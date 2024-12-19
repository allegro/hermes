import type { InactiveTopic } from '@/api/inactive-topics';

export const dummyInactiveTopics: InactiveTopic[] = [
  {
    topic: 'group.topic1',
    lastPublishedTsMs: 1732499845200,
    notificationTsMs: [1733499835210, 1733499645212],
    whitelisted: false,
  },
  {
    topic: 'group.topic2',
    lastPublishedTsMs: 1633928665148,
    notificationTsMs: [],
    whitelisted: true,
  },
];
