<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { subscriptionFqn } from '@/utils/subscription-utils/subscription-utils';
  import { useDialog } from '@/composables/dialog/use-dialog/useDialog';
  import { useI18n } from 'vue-i18n';
  import ConfirmationDialog from '@/components/confirmation-dialog/ConfirmationDialog.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';

  const { t } = useI18n();

  const props = defineProps<{
    topic: string;
    subscription: string;
    retransmitting: boolean;
    skippingAllMessages: boolean;
  }>();

  const emit = defineEmits<{
    retransmit: [fromDate: string];
    skipAllMessages: [];
  }>();

  const retransmitFrom = ref(new Date().toISOString().slice(0, -5));

  const subscriptionQualifiedName = subscriptionFqn(
    props.topic,
    props.subscription,
  );

  const {
    isDialogOpened: isRetransmitDialogOpened,
    actionButtonEnabled: actionRetransmitButtonEnabled,
    openDialog: openRetransmitDialog,
    closeDialog: closeRetransmitDialog,
    enableActionButton: enableRetransmitActionButton,
    disableActionButton: disableRetransmitActionButton,
  } = useDialog();

  const {
    isDialogOpened: isSkipAllMessagesDialogOpened,
    actionButtonEnabled: actionSkipAllMessagesButtonEnabled,
    openDialog: openSkipAllMessagesDialog,
    closeDialog: closeSkipAllMessagesDialog,
    enableActionButton: enableSkipAllMessagesActionButton,
    disableActionButton: disableSkipAllMessagesActionButton,
  } = useDialog();

  async function retransmit() {
    disableRetransmitActionButton();
    emit('retransmit', `${retransmitFrom.value}Z`);
    enableRetransmitActionButton();
    closeRetransmitDialog();
  }

  async function skipMessages() {
    disableSkipAllMessagesActionButton();
    emit('skipAllMessages');
    enableSkipAllMessagesActionButton();
    closeSkipAllMessagesDialog();
  }

  const retransmitText = computed(() =>
    t('subscription.confirmationDialog.retransmit.text', {
      subscriptionFqn: subscriptionQualifiedName,
      fromDate: retransmitFrom.value,
    }),
  );

  const skipAllMessagesText = computed(() =>
    t('subscription.confirmationDialog.skipAllMessages.text', {
      subscriptionFqn: subscriptionQualifiedName,
    }),
  );
</script>

<template>
  <confirmation-dialog
    v-model="isRetransmitDialogOpened"
    :actionButtonEnabled="actionRetransmitButtonEnabled"
    :title="$t('subscription.confirmationDialog.retransmit.title')"
    :text="retransmitText"
    @action="retransmit"
    @cancel="closeRetransmitDialog"
  />
  <confirmation-dialog
    v-model="isSkipAllMessagesDialogOpened"
    :actionButtonEnabled="actionSkipAllMessagesButtonEnabled"
    :title="$t('subscription.confirmationDialog.skipAllMessages.title')"
    :text="skipAllMessagesText"
    @action="skipMessages"
    @cancel="closeSkipAllMessagesDialog"
  />
  <v-card>
    <v-card-item class="border-b">
      <div>
        <v-card-title class="font-weight-bold">
          {{ $t('subscription.manageMessagesCard.title') }}
        </v-card-title>
        <v-card-subtitle>
          {{ $t('subscription.manageMessagesCard.subtitle') }}
        </v-card-subtitle>
      </div>
    </v-card-item>
    <v-form @submit.prevent>
      <v-row>
        <v-col md="6">
          <v-card-item>
            <p class="font-weight-bold">
              {{ $t('subscription.manageMessagesCard.retransmitTitle') }}
            </p>
            <v-row align="center" class="mt-2">
              <v-col>
                <v-text-field
                  variant="outlined"
                  density="compact"
                  :label="
                    $t(
                      'subscription.manageMessagesCard.retransmitStartTimestampLabel',
                    )
                  "
                  type="datetime-local"
                  v-model="retransmitFrom"
                  hide-details
                />
              </v-col>
              <v-col>
                <v-btn
                  :disabled="retransmitting || skippingAllMessages"
                  color="primary"
                  class="text-capitalize"
                  @click="openRetransmitDialog"
                  data-testid="retransmitButton"
                  :loading="retransmitting"
                >
                  <span>
                    {{ $t('subscription.manageMessagesCard.retransmitButton') }}
                  </span>
                </v-btn>
              </v-col>
            </v-row>
          </v-card-item>
        </v-col>

        <v-col md="6">
          <v-card-item>
            <p class="font-weight-bold">
              {{ $t('subscription.manageMessagesCard.skipAllMessagesTitle') }}
            </p>
            <p class="text-medium-emphasis">
              {{
                $t('subscription.manageMessagesCard.skipAllMessagesSubtitle')
              }}
            </p>
            <v-btn
              :disabled="retransmitting || skippingAllMessages"
              color="error"
              class="mt-2 text-capitalize"
              @click="openSkipAllMessagesDialog"
              data-testid="skipAllMessagesButton"
            >
              <loading-spinner v-if="skippingAllMessages"></loading-spinner>
              <span v-else>
                {{
                  $t('subscription.manageMessagesCard.skipAllMessagesButton')
                }}
              </span>
            </v-btn>
          </v-card-item>
        </v-col>
      </v-row>
    </v-form>
  </v-card>
</template>

<style scoped lang="scss"></style>
