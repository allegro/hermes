<script setup lang="ts">
  import { ProblemCode } from '@/api/subscription-health';
  import { useI18n } from 'vue-i18n';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import type { SubscriptionHealthProblem } from '@/api/subscription-health';

  const props = defineProps<{
    problems: SubscriptionHealthProblem[];
  }>();

  const { t } = useI18n();
</script>

<template>
  <console-alert
    v-if="props.problems.some(({ code }) => code === ProblemCode.LAGGING)"
    :title="t('subscription.healthProblemsAlerts.lagging.title')"
    :text="t('subscription.healthProblemsAlerts.lagging.text')"
    type="warning"
    icon="mdi-speedometer-slow"
    class="mb-2"
  />
  <console-alert
    v-if="
      props.problems.some(({ code }) => code === ProblemCode.MALFUNCTIONING)
    "
    :title="t('subscription.healthProblemsAlerts.malfunctioning.title')"
    :text="t('subscription.healthProblemsAlerts.malfunctioning.text')"
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
    :title="t('subscription.healthProblemsAlerts.malformedMessages.title')"
    :text="t('subscription.healthProblemsAlerts.malformedMessages.text')"
    type="warning"
    icon="mdi-alert"
    class="mb-2"
  />
  <console-alert
    v-if="props.problems.some(({ code }) => code === ProblemCode.TIMING_OUT)"
    :title="t('subscription.healthProblemsAlerts.timingOut.title')"
    :text="t('subscription.healthProblemsAlerts.timingOut.text')"
    type="warning"
    icon="mdi-clock-alert"
    class="mb-2"
  />
  <console-alert
    v-if="props.problems.some(({ code }) => code === ProblemCode.UNREACHABLE)"
    :title="t('subscription.healthProblemsAlerts.unreachable.title')"
    :text="t('subscription.healthProblemsAlerts.unreachable.text')"
    type="warning"
    icon="mdi-power-plug-off"
    class="mb-2"
  />
</template>

<style scoped lang="scss"></style>
