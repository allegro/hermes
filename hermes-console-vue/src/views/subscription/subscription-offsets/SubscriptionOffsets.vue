<script setup lang="ts">
  import { moveSubscriptionOffsets } from '@/api/hermes-client';
  import { ref } from 'vue';
  import { Subscription } from '@/api/subscription';
  import { useI18n } from 'vue-i18n';

  const props = defineProps<{
    subscription: Subscription;
  }>();

  const { t } = useI18n();

  const msg = ref<string>();

  function moveOffsets(): void {
    moveSubscriptionOffsets(
      props.subscription.topicName,
      props.subscription.name,
    )
      .then(() => (msg.value = t('subscription.moveOffsets.success')))
      .catch(
        (e) =>
          (msg.value = `${t('subscription.moveOffsets.failure')}, ${t(
            'subscription.moveOffsets.status',
          )}: ${e.response.status}, ${t(
            'subscription.moveOffsets.response',
          )}: ${JSON.stringify(e.response.data)}`),
      );
  }
</script>

<template>
  <v-card>
    <template #title>
      <p class="font-weight-bold">
        {{ $t('subscription.moveOffsets.title') }}
      </p>
      <v-btn color="red" class="mt-4" @click="moveOffsets">{{
        $t('subscription.moveOffsets.button')
      }}</v-btn>
      <!--      TODO: show a popup-->
      <p class="text-wrap">{{ msg }}</p>
    </template>
  </v-card>
</template>

<style scoped lang="scss"></style>
