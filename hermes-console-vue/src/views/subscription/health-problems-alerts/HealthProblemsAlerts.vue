<script setup lang="ts">
  import {
    ProblemCode,
    SubscriptionHealthProblem,
  } from '@/api/subscription-health';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';

  interface HealthProblemsAlertProps {
    problems: SubscriptionHealthProblem[];
  }

  const props = defineProps<HealthProblemsAlertProps>();

  const PROBLEMS_MESSAGES: Record<ProblemCode, string> = {
    [ProblemCode.LAGGING]:
      'Subscription lag is growing. Examine output rate and service response ' +
      'codes, looks like it is not consuming at full speed.',
    [ProblemCode.MALFUNCTIONING]:
      'Consuming service returns a lot of 5xx codes. Looks like it might be ' +
      "malfunctioning or doesn't know how to handle messages. Take a look " +
      'at "Last undelivered message" for more information.',
    [ProblemCode.RECEIVING_MALFORMED_MESSAGES]:
      'Consuming service returns a lot of 4xx codes. Maybe you are receiving ' +
      'some malformed messages? If this is normal behavior, switch Retry on ' +
      '4xx status flag to false. This way Hermes will not try to resend ' +
      'malformed messages, reducing traffic.',
    [ProblemCode.TIMING_OUT]:
      'Consuming service times out a lot. Hermes times out after 1 second, ' +
      'if you are not able to process message during this time, connection ' +
      'is reset and delivery fails.',
    [ProblemCode.UNREACHABLE]:
      'Unable to connect to consuming service instances. It is either network ' +
      'issue or your service instance is down.',
  };
</script>

<template>
  <console-alert
    v-if="props.problems.some(({ code }) => code === ProblemCode.LAGGING)"
    title="Subscription lagging"
    :text="PROBLEMS_MESSAGES[ProblemCode.LAGGING]"
    type="warning"
    icon="mdi-speedometer-slow"
    class="mb-2"
  />
  <console-alert
    v-if="
      props.problems.some(({ code }) => code === ProblemCode.MALFUNCTIONING)
    "
    title="Subscription malfunctioning"
    :text="PROBLEMS_MESSAGES[ProblemCode.MALFUNCTIONING]"
    type="warning"
    icon="mdi-alert"
    class="mb-2"
  />
  <console-alert
    v-if="
      props.problems.some(
        ({ code }) => code === ProblemCode.RECEIVING_MALFORMED_MESSAGES,
      )
    "
    title="Subscription receiving malformed messages"
    :text="PROBLEMS_MESSAGES[ProblemCode.RECEIVING_MALFORMED_MESSAGES]"
    type="warning"
    icon="mdi-alert"
    class="mb-2"
  />
  <console-alert
    v-if="props.problems.some(({ code }) => code === ProblemCode.TIMING_OUT)"
    title="Subscription timing out"
    :text="PROBLEMS_MESSAGES[ProblemCode.TIMING_OUT]"
    type="warning"
    icon="mdi-clock-alert"
    class="mb-2"
  />
  <console-alert
    v-if="props.problems.some(({ code }) => code === ProblemCode.UNREACHABLE)"
    title="Consuming service unreachable"
    :text="PROBLEMS_MESSAGES[ProblemCode.UNREACHABLE]"
    type="warning"
    icon="mdi-power-plug-off"
    class="mb-2"
  />
</template>

<style scoped lang="scss"></style>
