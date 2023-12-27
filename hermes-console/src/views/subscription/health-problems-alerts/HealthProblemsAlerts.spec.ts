import { ProblemCode } from '@/api/subscription-health';
import { render } from '@/utils/test-utils';
import HealthProblemsAlerts from '@/views/subscription/health-problems-alerts/HealthProblemsAlerts.vue';

describe('HealthProblemsAlerts', () => {
  it.each([
    [ProblemCode.LAGGING, 'lagging'],
    [ProblemCode.MALFUNCTIONING, 'malfunctioning'],
    [ProblemCode.RECEIVING_MALFORMED_MESSAGES, 'malformedMessages'],
    [ProblemCode.TIMING_OUT, 'timingOut'],
    [ProblemCode.UNREACHABLE, 'unreachable'],
  ])(
    'should show an appropriate health problem alert (%s)',
    (problemCode: ProblemCode, labelKey: string) => {
      // when
      const { getByText } = render(HealthProblemsAlerts, {
        props: {
          problems: [{ code: problemCode, description: 'something' }],
        },
      });

      // then
      expect(
        getByText(`subscription.healthProblemsAlerts.${labelKey}.title`),
      ).toBeVisible();
      expect(
        getByText(`subscription.healthProblemsAlerts.${labelKey}.text`),
      ).toBeVisible();
    },
  );
});
