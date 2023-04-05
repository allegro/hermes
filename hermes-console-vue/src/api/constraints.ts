export interface ConstraintsConfig {
  topicConstraints: Record<string, Constraint>;
  subscriptionConstraints: Record<string, Constraint>;
}

export interface Constraint {
  consumersNumber: number;
}
