import { ProblemCode } from '@/api/subscription-health';
import { render } from '@/utils/test-utils';
import HealthProblemsAlerts from '@/views/subscription/health-problems-alerts/HealthProblemsAlerts.vue';

describe('HealthProblemsAlerts', () => {
  it.each([
    [ProblemCode.LAGGING, /subscription lag is growing/i],
    [ProblemCode.MALFUNCTIONING, /a lot of 5xx codes/i],
    [ProblemCode.RECEIVING_MALFORMED_MESSAGES, /a lot of 4xx codes/i],
    [ProblemCode.TIMING_OUT, /times out a lot/i],
    [ProblemCode.UNREACHABLE, /unable to connect/i],
  ])(
    'should show an appropriate health problem alert (%s)',
    (problemCode: ProblemCode, warningMessage: RegExp) => {
      // when
      const { getByText } = render(HealthProblemsAlerts, {
        props: {
          problems: [{ code: problemCode, description: 'something' }],
        },
      });

      // then
      expect(getByText(warningMessage)).toBeInTheDocument();
    },
  );
});
