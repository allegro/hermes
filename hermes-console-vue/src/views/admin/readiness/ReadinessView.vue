<script setup lang="ts">
  import { useI18n } from 'vue-i18n';
  import { useReadiness } from '@/composables/use-readiness/useReadiness';
  import ConsoleAlert from '@/components/console-alert/ConsoleAlert.vue';
  import LoadingSpinner from '@/components/loading-spinner/LoadingSpinner.vue';
  import ReadinessBreadcrumbs from '@/views/admin/readiness/readiness-breadcrumbs/ReadinessBreadcrumbs.vue';

  const { t } = useI18n();

  const { datacentersReadiness, loading, error } = useReadiness();
</script>

<template>
  <v-container fill-height fluid class="mx-auto">
    <v-row dense>
      <v-col md="12">
        <readiness-breadcrumbs />
        <loading-spinner v-if="loading" />
        <console-alert
          v-if="error"
          :title="t('readiness.connectionError.title')"
          :text="t('readiness.connectionError.text')"
          type="error"
        />
      </v-col>
    </v-row>
    <v-container v-if="!loading && !error">
      <v-row dense>
        <v-col md="12">
          <p class="text-h4 font-weight-bold mb-3">
            {{ t('readiness.title') }}
          </p>
        </v-col>
      </v-row>
      <v-row dense>
        <v-col md="12">
          <v-table>
            <thead>
              <tr>
                <th class="text-left">{{ t('readiness.index') }}</th>
                <th class="text-left">{{ t('readiness.datacenter') }}</th>
                <th class="text-left">{{ t('readiness.isReady') }}</th>
                <th class="text-left">{{ t('readiness.control') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in datacentersReadiness" :key="index">
                <td>{{ index + 1 }}</td>
                <td>{{ item.datacenter }}</td>
                <td>{{ item.isReady }}</td>
                <td class="w-0">
                  <v-btn
                    variant="text"
                    v-if="!item.isReady"
                    prepend-icon="mdi-console-line"
                    color="green"
                    block
                  >
                    {{ t('readiness.turnOn') }}</v-btn
                  >
                  <v-btn
                    variant="text"
                    v-if="item.isReady"
                    prepend-icon="mdi-console-line"
                    color="red"
                    block
                  >
                    {{ t('readiness.turnOff') }}</v-btn
                  >
                </td>
              </tr>
            </tbody>
          </v-table>
        </v-col>
      </v-row>
    </v-container>
  </v-container>
</template>

<style lang="scss"></style>
