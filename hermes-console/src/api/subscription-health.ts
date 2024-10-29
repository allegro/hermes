/* eslint-disable no-unused-vars */
export interface SubscriptionHealth {
  status: Status;
  problems: SubscriptionHealthProblem[];
}

export const enum Status {
  HEALTHY = 'HEALTHY',
  UNHEALTHY = 'UNHEALTHY',
  NO_DATA = 'NO_DATA',
}

export interface SubscriptionHealthProblem {
  code: ProblemCode;
  description: string;
}

export const enum ProblemCode {
  LAGGING = 'LAGGING',
  UNREACHABLE = 'UNREACHABLE',
  TIMING_OUT = 'TIMING_OUT',
  MALFUNCTIONING = 'MALFUNCTIONING',
  RECEIVING_MALFORMED_MESSAGES = 'RECEIVING_MALFORMED_MESSAGES',
}
