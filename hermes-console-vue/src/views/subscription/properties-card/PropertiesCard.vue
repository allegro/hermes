<script setup lang="ts">
  import { DeliveryType } from '@/api/subscription';
  import { formatTimestamp } from '@/utils/date-formatter/date-formatter';
  import { useI18n } from 'vue-i18n';
  import KeyValueCard from '@/components/key-value-card/KeyValueCard.vue';
  import KeyValueCardItem from '@/components/key-value-card/key-value-card-item/KeyValueCardItem.vue';
  import type { Subscription } from '@/api/subscription';

  const props = defineProps<{
    subscription: Subscription;
  }>();

  const { t } = useI18n();

  function getTrackingModeName(trackingMode: string): string {
    switch (trackingMode) {
      case 'trackingOff':
        return t('subscription.propertiesCard.trackingOff');
      case 'discardedOnly':
        return t('subscription.propertiesCard.discardedOnly');
      case 'trackingAll':
        return t('subscription.propertiesCard.trackingAll');
      default:
        return t('subscription.propertiesCard.unknown');
    }
  }
</script>

<template>
  <key-value-card :title="$t('subscription.propertiesCard.title')">
    <key-value-card-item
      :name="$t('subscription.propertiesCard.contentType')"
      :value="props.subscription.contentType"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.deliveryType')"
      :value="props.subscription.deliveryType"
      :tooltip="$t('subscription.propertiesCard.tooltips.deliveryType')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.mode')"
      :value="props.subscription.mode"
      :tooltip="$t('subscription.propertiesCard.tooltips.mode')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.rateLimit')"
      :value="props.subscription.subscriptionPolicy.rate"
      v-if="props.subscription.deliveryType === DeliveryType.SERIAL"
      :tooltip="$t('subscription.propertiesCard.tooltips.rateLimit')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.batchSize')"
      :value="props.subscription.subscriptionPolicy.batchSize"
      v-if="props.subscription.deliveryType === DeliveryType.BATCH"
      :tooltip="$t('subscription.propertiesCard.tooltips.batchSize')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.batchTime')"
      :value="`${props.subscription.subscriptionPolicy.batchTime} ms`"
      v-if="props.subscription.deliveryType === DeliveryType.BATCH"
      :tooltip="$t('subscription.propertiesCard.tooltips.batchTime')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.batchVolume')"
      :value="`${props.subscription.subscriptionPolicy.batchVolume} B`"
      v-if="props.subscription.deliveryType === DeliveryType.BATCH"
      :toolti="$t('subscription.propertiesCard.tooltips.batchVolume')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.requestTimeout')"
      :value="`${props.subscription.subscriptionPolicy.requestTimeout} ms`"
      v-if="props.subscription.deliveryType === DeliveryType.BATCH"
      :tooltip="$t('subscription.propertiesCard.tooltips.requestTimeout')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.sendingDelay')"
      :value="`${props.subscription.subscriptionPolicy.sendingDelay} ms`"
      v-if="props.subscription.deliveryType === DeliveryType.SERIAL"
      :tooltip="$t('subscription.propertiesCard.tooltips.sendingDelay')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.messageTtl')"
      :value="`${props.subscription.subscriptionPolicy.messageTtl} s`"
      :tooltip="$t('subscription.propertiesCard.tooltips.messageTtl')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.requestTimeout')"
      :value="`${props.subscription.subscriptionPolicy.requestTimeout} ms`"
      :tooltip="$t('subscription.propertiesCard.tooltips.requestTimeout')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.trackingMode')"
      :value="getTrackingModeName(props.subscription.trackingMode)"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.retryClientErrors')"
      :value="props.subscription.subscriptionPolicy.retryClientErrors"
      :tooltip="$t('subscription.propertiesCard.tooltips.retryClientErrors')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.retryBackoff')"
      :value="`${props.subscription.subscriptionPolicy.messageBackoff} ms`"
      :tooltip="$t('subscription.propertiesCard.tooltips.retryBackoff')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.backoffMultiplier')"
      :value="props.subscription.subscriptionPolicy.backoffMultiplier"
      v-if="props.subscription.deliveryType === DeliveryType.SERIAL"
      :tooltip="$t('subscription.propertiesCard.tooltips.backoffMultiplier')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.backoffMaxInterval')"
      :value="`${props.subscription.subscriptionPolicy.backoffMaxIntervalInSec} s`"
      v-if="
        props.subscription.deliveryType === DeliveryType.SERIAL &&
        props.subscription.subscriptionPolicy.backoffMultiplier > 1
      "
      :tooltip="$t('subscription.propertiesCard.tooltips.backoffMaxInterval')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.monitoringSeverity')"
      :value="props.subscription.monitoringDetails.severity"
      :tooltip="$t('subscription.propertiesCard.tooltips.monitoringSeverity')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.monitoringReaction')"
      :value="props.subscription.monitoringDetails.reaction"
      tooltip="Information for monitoring how to react when the subscription becomes unhealthy (e.g. team name or Pager Duty ID)."
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.http2')"
      :value="props.subscription.http2Enabled"
      :tooltip="$t('subscription.propertiesCard.tooltips.http2')"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.subscriptionIdentityHeaders')"
      :value="props.subscription.subscriptionIdentityHeadersEnabled"
      :tooltip="
        $t('subscription.propertiesCard.tooltips.subscriptionIdentityHeaders')
      "
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.autoDeleteWithTopic')"
      :value="props.subscription.autoDeleteWithTopicEnabled"
      :tooltip="$t('subscription.propertiesCard.tooltips.autoDeleteWithTopic')"
    />
    <!--    /*-->
    <!--    * TODO: subscription.html-->
    <!--    *-->
    <!--    * <p ng-repeat="(key, entry) in endpointAddressResolverMetadataConfig">-->
    <!--    *     <strong>{{entry.title}}:</strong>-->
    <!--    *     {{entry.options[subscription.endpointAddressResolverMetadata[key]] || subscription.endpointAddressResolverMetadata[key]}}-->
    <!--    *     <span ng-show="entry.hint" uib-popover="{{entry.hint}}" popover-trigger="mouseenter" class="fa helpme pull-right">&#xf128;</span>-->
    <!--    * </p>-->
    <!--    * <p ng-repeat="(key, value) in notSupportedEndpointAddressResolverMetadataEntries(subscription.endpointAddressResolverMetadata)">-->
    <!--    *     <strong>{{key}}:</strong> {{value}}-->
    <!--    * </p>-->
    <!--    */-->
    <key-value-card-item
      :name="$t('subscription.propertiesCard.createdAt')"
      :value="formatTimestamp(props.subscription.createdAt)"
    />
    <key-value-card-item
      :name="$t('subscription.propertiesCard.modifiedAt')"
      :value="formatTimestamp(props.subscription.modifiedAt)"
    />
  </key-value-card>
</template>

<style scoped lang="scss"></style>
