export function formatNumber(
  number: number | string,
  fractionDigits: number = 0,
): string {
  return Number(number).toLocaleString('en-US', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  });
}
