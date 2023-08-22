export function groupName(topicName: string): string {
  const topicStartIdx = topicName.lastIndexOf('.');
  return topicName.substring(0, topicStartIdx);
}
