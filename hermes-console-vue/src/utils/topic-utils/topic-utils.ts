export function groupName(topicName: string): string {
  const topicStartIdx = topicName.lastIndexOf('.');
  return topicName.substring(0, topicStartIdx);
}

export function topicQualifiedName(
  groupName: string,
  topicName: string,
): string {
  return `${groupName}.${topicName}`;
}
