export interface ConstraintsConfig {
  topicConstraints: Record<string, Constraint>;
  subscriptionConstraints: Record<string, Constraint>;
}

export interface Constraint {
  consumersNumber: number;
  reason: string;
}

export interface SubscriptionConstraint {
  subscriptionName: string;
  constraints: Constraint;
}

export interface TopicConstraint {
  topicName: string;
  constraints: Constraint;
}
