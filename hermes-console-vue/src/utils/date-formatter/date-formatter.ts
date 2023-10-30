export function formatTimestamp(timestamp: number): string {
  return new Date(timestamp * 1000)
    .toISOString()
    .replace('T', ' ')
    .replace('Z', '')
    .slice(0, 19);
}

export function formatTimestampMillis(timestamp: number): string {
  return new Date(timestamp)
    .toISOString()
    .replace('T', ' ')
    .replace('Z', '')
    .slice(0, 19);
}
