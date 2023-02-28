import { ContentType } from '@/api/content-type';
import { DeliveryType, Severity, SubscriptionMode } from '@/api/subscription';
import { dummySubscription } from '@/dummy/subscription';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import PropertiesCard from '@/views/subscription/properties-card/PropertiesCard.vue';

describe('PropertiesCard', () => {
  it('should render subscription properties card', () => {
    // given
    const props = {
      subscription: dummySubscription,
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    expect(getByText('subscription.propertiesCard.title')).toBeInTheDocument();
  });

  it.each([
    [ContentType.AVRO, 'AVRO'],
    [ContentType.JSON, 'JSON'],
  ])(
    'should render subscription content type (%s)',
    (contentType, contentTypeText) => {
      // given
      const props = {
        subscription: { ...dummySubscription, contentType },
      };

      // when
      const { getByText } = render(PropertiesCard, { props });

      // then
      const contentTypeRow = getByText(
        'subscription.propertiesCard.contentType',
      ).closest('tr')!;
      expect(
        within(contentTypeRow).getByText(contentTypeText),
      ).toBeInTheDocument();
    },
  );

  it.each([
    [DeliveryType.SERIAL, 'SERIAL'],
    [DeliveryType.BATCH, 'BATCH'],
  ])(
    'should render subscription delivery type (%s)',
    (deliveryType: DeliveryType, deliveryTypeText: string) => {
      // given
      const props = {
        subscription: { ...dummySubscription, deliveryType },
      };

      // when
      const { getByText } = render(PropertiesCard, { props });

      // then
      const deliveryTypeRow = getByText(
        'subscription.propertiesCard.deliveryType',
      ).closest('tr')!;
      expect(
        within(deliveryTypeRow).getByText(deliveryTypeText),
      ).toBeInTheDocument();
    },
  );

  it.each([
    [SubscriptionMode.ANYCAST, 'ANYCAST'],
    [SubscriptionMode.BROADCAST, 'BROADCAST'],
  ])(
    'should render subscription mode (%s)',
    (mode: SubscriptionMode, modeText: string) => {
      // given
      const props = {
        subscription: { ...dummySubscription, mode },
      };

      // when
      const { getByText } = render(PropertiesCard, { props });

      // then
      const modeRow = getByText('subscription.propertiesCard.mode').closest(
        'tr',
      )!;
      expect(within(modeRow).getByText(modeText)).toBeInTheDocument();
    },
  );

  it('should render subscription rate limit for SERIAL delivery type', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        deliveryType: DeliveryType.SERIAL,
        subscriptionPolicy: {
          ...dummySubscription.subscriptionPolicy,
          rate: 15,
        },
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const rateLimitRow = getByText(
      'subscription.propertiesCard.rateLimit',
    ).closest('tr')!;
    expect(within(rateLimitRow).getByText('15')).toBeInTheDocument();
  });

  it('should render subscription batch size for BATCH delivery type', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        deliveryType: DeliveryType.BATCH,
        subscriptionPolicy: {
          batchSize: 10,
        },
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const batchSizeRow = getByText(
      'subscription.propertiesCard.batchSize',
    ).closest('tr')!;
    expect(within(batchSizeRow).getByText('10')).toBeInTheDocument();
  });

  it('should render subscription batch time window for BATCH delivery type', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        deliveryType: DeliveryType.BATCH,
        subscriptionPolicy: {
          batchTime: 500,
        },
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const batchTimeRow = getByText(
      'subscription.propertiesCard.batchTime',
    ).closest('tr')!;
    expect(within(batchTimeRow).getByText('500 ms')).toBeInTheDocument();
  });

  it('should render subscription batch volume for BATCH delivery type', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        deliveryType: DeliveryType.BATCH,
        subscriptionPolicy: {
          batchVolume: 1024,
        },
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const batchVolumeRow = getByText(
      'subscription.propertiesCard.batchVolume',
    ).closest('tr')!;
    expect(within(batchVolumeRow).getByText('1024 B')).toBeInTheDocument();
  });

  it('should not render batch-specific data for SERIAL delivery type', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        deliveryType: DeliveryType.SERIAL,
      },
    };

    // when
    const { queryByText } = render(PropertiesCard, { props });

    // then
    expect(
      queryByText('subscription.propertiesCard.batchSize'),
    ).not.toBeInTheDocument();
    expect(
      queryByText('subscription.propertiesCard.batchTime'),
    ).not.toBeInTheDocument();
    expect(
      queryByText('subscription.propertiesCard.batchVolume'),
    ).not.toBeInTheDocument();
  });

  it('should render subscription sending delay for SERIAL delivery type', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        deliveryType: DeliveryType.SERIAL,
        subscriptionPolicy: {
          sendingDelay: 200,
        },
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const batchVolumeRow = getByText(
      'subscription.propertiesCard.sendingDelay',
    ).closest('tr')!;
    expect(within(batchVolumeRow).getByText('200 ms')).toBeInTheDocument();
  });

  it('should not render SERIAL delivery specific data for BATCH delivery type', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        deliveryType: DeliveryType.BATCH,
      },
    };

    // when
    const { queryByText } = render(PropertiesCard, { props });

    // then
    expect(
      queryByText('subscription.propertiesCard.rateLimit'),
    ).not.toBeInTheDocument();
    expect(
      queryByText('subscription.propertiesCard.sendingDelay'),
    ).not.toBeInTheDocument();
    expect(
      queryByText('subscription.propertiesCard.backoffMultiplier'),
    ).not.toBeInTheDocument();
    expect(
      queryByText('subscription.propertiesCard.backoffMaxInterval'),
    ).not.toBeInTheDocument();
  });

  it('should render subscription message TTL', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        subscriptionPolicy: {
          messageTtl: 60,
        },
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const batchVolumeRow = getByText(
      'subscription.propertiesCard.messageTtl',
    ).closest('tr')!;
    expect(within(batchVolumeRow).getByText('60 s')).toBeInTheDocument();
  });

  it('should render subscription request timeout', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        subscriptionPolicy: {
          requestTimeout: 1000,
        },
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const batchVolumeRow = getByText(
      'subscription.propertiesCard.requestTimeout',
    ).closest('tr')!;
    expect(within(batchVolumeRow).getByText('1000 ms')).toBeInTheDocument();
  });

  it.each([
    ['trackingOff', 'subscription.propertiesCard.trackingOff'],
    ['discardedOnly', 'subscription.propertiesCard.discardedOnly'],
    ['trackingAll', 'subscription.propertiesCard.trackingAll'],
    ['someUnknownValue', 'subscription.propertiesCard.unknown'],
  ])(
    'should render subscription message delivery tracking (%s)',
    (trackingMode, trackingModeName) => {
      // given
      const props = {
        subscription: {
          ...dummySubscription,
          trackingMode,
        },
      };

      // when
      const { getByText } = render(PropertiesCard, { props });

      // then
      const trackingRow = getByText(
        'subscription.propertiesCard.trackingMode',
      ).closest('tr')!;
      expect(
        within(trackingRow).getByText(trackingModeName),
      ).toBeInTheDocument();
    },
  );

  it.each([true, false])(
    'should render subscription retry client errors flag (%s)',
    (retryClientErrors) => {
      // given
      const props = {
        subscription: {
          ...dummySubscription,
          subscriptionPolicy: {
            ...dummySubscription.subscriptionPolicy,
            retryClientErrors,
          },
        },
      };

      // when
      const { getByText } = render(PropertiesCard, { props });

      // then
      const retry4xxRow = getByText(
        'subscription.propertiesCard.retryClientErrors',
      ).closest('tr')!;
      expect(
        within(retry4xxRow).getByText(retryClientErrors.toString()),
      ).toBeInTheDocument();
    },
  );

  it('should render subscription retry backoff', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        subscriptionPolicy: {
          ...dummySubscription.subscriptionPolicy,
          messageBackoff: 150,
        },
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const retryBackoffRow = getByText(
      'subscription.propertiesCard.retryBackoff',
    ).closest('tr')!;
    expect(within(retryBackoffRow).getByText('150 ms')).toBeInTheDocument();
  });

  it('should render subscription retry backoff multiplier for SERIAL delivery type', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        deliveryType: DeliveryType.SERIAL,
        subscriptionPolicy: {
          ...dummySubscription.subscriptionPolicy,
          backoffMultiplier: 1.0,
        },
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const retryBackoffMultiplierRow = getByText(
      'subscription.propertiesCard.backoffMultiplier',
    ).closest('tr')!;
    expect(
      within(retryBackoffMultiplierRow).getByText('1'),
    ).toBeInTheDocument();
  });

  it('should retry backoff max interval', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        deliveryType: DeliveryType.SERIAL,
        subscriptionPolicy: {
          ...dummySubscription.subscriptionPolicy,
          backoffMultiplier: 2.0,
          backoffMaxIntervalInSec: 400,
        },
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const retryBackoffMaxIntervalRow = getByText(
      'subscription.propertiesCard.backoffMaxInterval',
    ).closest('tr')!;
    expect(within(retryBackoffMaxIntervalRow).getByText('400 s'));
  });

  it.each([
    [Severity.CRITICAL, 'CRITICAL'],
    [Severity.IMPORTANT, 'IMPORTANT'],
    [Severity.NON_IMPORTANT, 'NON_IMPORTANT'],
  ])(
    'should render subscription monitoring severity (%s)',
    (severity: Severity, severityName: string) => {
      // given
      const props = {
        subscription: {
          ...dummySubscription,
          monitoringDetails: {
            ...dummySubscription.monitoringDetails,
            severity,
          },
        },
      };

      // when
      const { getByText } = render(PropertiesCard, { props });

      // then
      const severityRow = getByText(
        'subscription.propertiesCard.monitoringSeverity',
      ).closest('tr')!;
      expect(within(severityRow).getByText(severityName)).toBeInTheDocument();
    },
  );

  it('should render subscription monitoring reaction', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        monitoringDetails: {
          ...dummySubscription.monitoringDetails,
          reaction: 'foo',
        },
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const reactionRow = getByText(
      'subscription.propertiesCard.monitoringReaction',
    ).closest('tr')!;
    expect(within(reactionRow).getByText('foo')).toBeInTheDocument();
  });

  it.each([true, false])(
    'should render subscription http/2 configuration flag (%s)',
    (http2Enabled: boolean) => {
      // given
      const props = {
        subscription: {
          ...dummySubscription,
          http2Enabled,
        },
      };

      // when
      const { getByText } = render(PropertiesCard, { props });

      // then
      const http2Row = getByText('subscription.propertiesCard.http2').closest(
        'tr',
      )!;
      expect(
        within(http2Row).getByText(http2Enabled.toString()),
      ).toBeInTheDocument();
    },
  );

  it.each([true, false])(
    'should render attach subscription identity headers flag',
    (subscriptionIdentityHeadersEnabled) => {
      // given
      const props = {
        subscription: {
          ...dummySubscription,
          subscriptionIdentityHeadersEnabled,
        },
      };

      // when
      const { getByText } = render(PropertiesCard, { props });

      // then
      const subscriptionIdentityHeadersRow = getByText(
        'subscription.propertiesCard.subscriptionIdentityHeaders',
      ).closest('tr')!;
      expect(
        within(subscriptionIdentityHeadersRow).getByText(
          subscriptionIdentityHeadersEnabled.toString(),
        ),
      ).toBeInTheDocument();
    },
  );

  it.each([true, false])(
    'should render subscription auto remove flag',
    (autoDeleteWithTopicEnabled: boolean) => {
      // given
      const props = {
        subscription: {
          ...dummySubscription,
          autoDeleteWithTopicEnabled,
        },
      };

      // when
      const { getByText } = render(PropertiesCard, { props });

      // then
      const autoDeleteRow = getByText(
        'subscription.propertiesCard.autoDeleteWithTopic',
      ).closest('tr')!;
      expect(
        within(autoDeleteRow).getByText(autoDeleteWithTopicEnabled.toString()),
      ).toBeInTheDocument();
    },
  );

  it('should render subscription creation date', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        createdAt: 1676735681,
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const createdAtRow = getByText(
      'subscription.propertiesCard.createdAt',
    ).closest('tr')!;
    expect(
      within(createdAtRow).getByText('2023-02-18 15:54:41'),
    ).toBeInTheDocument();
  });

  it('should render subscription modification date', () => {
    // given
    const props = {
      subscription: {
        ...dummySubscription,
        modifiedAt: 1676735697,
      },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const modifiedAtRow = getByText(
      'subscription.propertiesCard.modifiedAt',
    ).closest('tr')!;
    expect(
      within(modifiedAtRow).getByText('2023-02-18 15:54:57'),
    ).toBeInTheDocument();
  });
});
