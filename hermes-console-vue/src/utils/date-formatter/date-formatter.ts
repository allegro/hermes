export function dateFromTimestamp(timestamp: number): string {
  return new Date(timestamp * 1000)
    .toISOString()
    .replace('T', ' ')
    .replace('Z', '')
    .slice(0, 19);
}
