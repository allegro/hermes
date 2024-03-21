export function formatTimestamp(timestamp: number): string {
  try {
    return new Date(timestamp * 1000)
      .toISOString()
      .replace('T', ' ')
      .replace('Z', '')
      .slice(0, 19);
  } catch (ignore) {
    return 'Invalid format';
  }
}

export function formatTimestampMillis(timestamp: number): string {
  try {
    return new Date(timestamp)
      .toISOString()
      .replace('T', ' ')
      .replace('Z', '')
      .slice(0, 19);
  } catch (ignore) {
    return 'Invalid format';
  }
}
