import { formatNumber } from '@/utils/number-formatter/number-formatter';

describe('number-formatter', () => {
  it('should format number with fraction part', () => {
    // given
    const number = 42.21937;

    // when
    const formattedNumber = formatNumber(number, 2);

    // then
    expect(formattedNumber).toBe('42.22');
  });

  it('should format number with thousands separators and no fraction part', () => {
    // given
    const number = 59454123997;

    // when
    const formattedNumber = formatNumber(number, 0);

    // then
    expect(formattedNumber).toBe('59,454,123,997');
  });

  it('should format number with thousands separators and fraction part', () => {
    // given
    const number = 59454123997.8141;

    // when
    const formattedNumber = formatNumber(number, 3);

    // then
    expect(formattedNumber).toBe('59,454,123,997.814');
  });
});
