export interface SubscriptionName {
  topicName: string;
  subscriptionName: string;
}

export function parseSubscriptionFqn(
  subscriptionFqn: string,
): SubscriptionName {
  const tokens = subscriptionFqn.split('$');
  if (tokens.length != 2) {
    throw new Error(
      `Failed to parse subscriptionFqn: ${subscriptionFqn}, expected format: 'topicName$subscriptionName'`,
    );
  }
  return {
    topicName: tokens[0],
    subscriptionName: tokens[1],
  };
}

export function subscriptionFqn(topicName: string, subscriptionName: string) {
  return `${topicName}$${subscriptionName}`;
}
