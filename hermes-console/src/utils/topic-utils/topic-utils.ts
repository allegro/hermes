export function groupName(topicName: string): string {
  const topicStartIdx = topicName.lastIndexOf('.');
  return topicName.substring(0, topicStartIdx);
}

export function topicName(qualifiedTopicName: string): string {
  const topicStartIdx = qualifiedTopicName.lastIndexOf('.');
  return qualifiedTopicName.substring(topicStartIdx + 1);
}

export function topicQualifiedName(
  groupName: string,
  topicName: string,
): string {
  return `${groupName}.${topicName}`;
}
