import { formatNumber } from '@/utils/number-formatter/number-formatter';

describe('number-formatter', () => {
  it.each([
    { number: 42.21937, expectedFormatted: '42.22' },
    { number: 42.21137, expectedFormatted: '42.21' },
    { number: 3.1415926535, expectedFormatted: '3.14' },
    { number: 0.001234, expectedFormatted: '0.00' },
  ])('should format number with fraction part', () => {
    // given
    const number = 42.21937;

    // when
    const formattedNumber = formatNumber(number, 2);

    // then
    expect(formattedNumber).toBe('42.22');
  });

  it.each([
    { number: 2001, expectedFormatted: '2,001' },
    { number: 19741, expectedFormatted: '19,741' },
    { number: 1500100900, expectedFormatted: '1,500,100,900' },
    { number: 59454123997, expectedFormatted: '59,454,123,997' },
  ])(
    'should format number with thousands separators and no fraction part (%d)',
    ({ number, expectedFormatted }) => {
      // when
      const formattedNumber = formatNumber(number, 0);

      // then
      expect(formattedNumber).toBe(expectedFormatted);
    },
  );

  it.each([
    { number: 2001.94913, expectedFormatted: '2,001.949' },
    { number: 19741.21279, expectedFormatted: '19,741.213' },
    { number: 1500100900.001, expectedFormatted: '1,500,100,900.001' },
    { number: 59454123997.0001, expectedFormatted: '59,454,123,997.000' },
  ])(
    'should format number with thousands separators and fraction part (%d)',
    ({ number, expectedFormatted }) => {
      // when
      const formattedNumber = formatNumber(number, 3);

      // then
      expect(formattedNumber).toBe(expectedFormatted);
    },
  );
});
