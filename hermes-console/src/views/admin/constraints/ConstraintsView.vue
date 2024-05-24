<script setup lang="ts">
  import { ref } from 'vue';
  import { useConstraints } from '@/composables/constraints/use-constraints/useConstraints';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import ConstraintsListing from '@/views/admin/constraints/constraints-listing/ConstraintsListing.vue';
  import CreateConstraintDialog from '@/views/admin/constraints/create-constraint-form/CreateConstraintDialog.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import type { Constraint } from '@/api/constraints';

  const { t } = useI18n();
  const router = useRouter();

  const topicFilter = ref<string>();
  const subscriptionFilter = ref<string>();

  const {
    topicConstraints,
    subscriptionConstraints,
    upsertTopicConstraint,
    deleteTopicConstraint,
    upsertSubscriptionConstraint,
    deleteSubscriptionConstraint,
    loading,
    error,
  } = useConstraints();

  const breadcrumbsItems = [
    {
      title: t('constraints.breadcrumbs.home'),
      href: '/',
    },
    {
      title: t('constraints.breadcrumbs.title'),
    },
  ];

  const refreshOnMutation = (mutated: boolean) => {
    if (mutated) {
      router.go(0);
    }
  };

  const onTopicConstraintUpserted = async (
    topicName: string,
    constraint: Constraint,
  ) => {
    const topicDeleted = await upsertTopicConstraint(topicName, constraint);
    refreshOnMutation(topicDeleted);
  };

  const onTopicConstraintDeleted = async (topicName: string) => {
    const subscriptionDeleted = await deleteTopicConstraint(topicName);
    refreshOnMutation(subscriptionDeleted);
  };

  const onSubscriptionConstraintCreated = async (
    subscriptionFqn: string,
    constraint: Constraint,
  ) => {
    const constraintChanged = await upsertSubscriptionConstraint(
      subscriptionFqn,
      constraint,
    );
    refreshOnMutation(constraintChanged);
  };

  const onSubscriptionConstraintDeleted = async (subscriptionFqn: string) => {
    const constraintChanged =
      await deleteSubscriptionConstraint(subscriptionFqn);
    refreshOnMutation(constraintChanged);
  };
</script>

<template>
  <v-container>
    <v-row dense>
      <v-col md="12">
        <v-breadcrumbs :items="breadcrumbsItems" density="compact" />
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error.fetchConstraints"
          :title="$t('constraints.connectionError.title')"
          :text="$t('constraints.connectionError.text')"
          type="error"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="10">
        <p class="text-h4 font-weight-bold mb-3">
          {{ $t('constraints.topicConstraints.heading') }}
        </p>
      </v-col>
      <v-col md="2">
        <v-btn
          prepend-icon="mdi-plus"
          color="secondary"
          block
          data-testid="addTopicConstraint"
        >
          {{ $t('constraints.topicConstraints.actions.create') }}
          <CreateConstraintDialog
            :isSubscription="false"
            @create="onTopicConstraintUpserted"
          >
          </CreateConstraintDialog>
        </v-btn>
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <v-text-field
          single-line
          :label="$t('constraints.topicConstraints.actions.search')"
          density="compact"
          v-model="topicFilter"
          prepend-inner-icon="mdi-magnify"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <constraints-listing
          v-if="topicConstraints"
          :constraints="topicConstraints"
          :filter="topicFilter"
          @update="onTopicConstraintUpserted"
          @delete="onTopicConstraintDeleted"
        />
      </v-col>
    </v-row>
    <v-row dense class="mt-10">
      <v-col md="10">
        <p class="text-h4 font-weight-bold mb-3">
          {{ $t('constraints.subscriptionConstraints.heading') }}
        </p>
      </v-col>
      <v-col md="2">
        <v-btn
          prepend-icon="mdi-plus"
          color="secondary"
          block
          data-testid="addSubscriptionConstraint"
        >
          {{ $t('constraints.subscriptionConstraints.actions.create') }}
          <CreateConstraintDialog
            :isSubscription="true"
            @create="onSubscriptionConstraintCreated"
          >
          </CreateConstraintDialog>
        </v-btn>
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <v-text-field
          single-line
          :label="$t('constraints.subscriptionConstraints.actions.search')"
          density="compact"
          v-model="subscriptionFilter"
          prepend-inner-icon="mdi-magnify"
        />
      </v-col>
    </v-row>
    <v-row dense>
      <v-col md="12">
        <constraints-listing
          v-if="subscriptionConstraints"
          :constraints="subscriptionConstraints"
          :filter="subscriptionFilter"
          @update="onSubscriptionConstraintCreated"
          @delete="onSubscriptionConstraintDeleted"
        />
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped lang="scss"></style>
