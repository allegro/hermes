export interface ConstraintsConfig {
  topicConstraints: Record<string, Constraint>;
  subscriptionConstraints: Record<string, Constraint>;
}

export interface Constraint {
  consumersNumber: number;
}

export interface SubscriptionConstraint {
  subscriptionName: string;
  constraint: Constraint;
}

export interface TopicConstraint {
  topicName: string;
  constraint: Constraint;
}
