import type { ConstraintsConfig } from '@/api/constraints';

export const dummyConstraints: ConstraintsConfig = {
  topicConstraints: {
    'pl.group.Topic1': {
      consumersNumber: 2,
    },
    'pl.group.Topic2': {
      consumersNumber: 4,
    },
  },
  subscriptionConstraints: {
    'pl.group.Topic$subscription1': {
      consumersNumber: 6,
    },
    'pl.group.Topic$subscription2': {
      consumersNumber: 8,
    },
  },
};
