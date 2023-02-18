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
    expect(getByText('Properties')).toBeInTheDocument();
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
      const contentTypeRow = getByText('Content type').closest('tr')!;
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
    (deliveryType, deliveryTypeText) => {
      // given
      const props = {
        subscription: { ...dummySubscription, deliveryType },
      };

      // when
      const { getByText } = render(PropertiesCard, { props });

      // then
      const deliveryTypeRow = getByText('Delivery type').closest('tr')!;
      expect(
        within(deliveryTypeRow).getByText(deliveryTypeText),
      ).toBeInTheDocument();
    },
  );

  it.each([
    [SubscriptionMode.ANYCAST, 'ANYCAST'],
    [SubscriptionMode.BROADCAST, 'BROADCAST'],
  ])('should render subscription mode (%s)', (mode, modeText) => {
    // given
    const props = {
      subscription: { ...dummySubscription, mode },
    };

    // when
    const { getByText } = render(PropertiesCard, { props });

    // then
    const modeRow = getByText('Mode').closest('tr')!;
    expect(within(modeRow).getByText(modeText)).toBeInTheDocument();
  });

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
    const rateLimitRow = getByText('Rate limit').closest('tr')!;
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
    const batchSizeRow = getByText('Batch size').closest('tr')!;
    expect(within(batchSizeRow).getByText('10 messages')).toBeInTheDocument();
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
    const batchTimeRow = getByText('Batch time window').closest('tr')!;
    expect(
      within(batchTimeRow).getByText('500 milliseconds'),
    ).toBeInTheDocument();
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
    const batchVolumeRow = getByText('Batch volume').closest('tr')!;
    expect(within(batchVolumeRow).getByText('1024 bytes')).toBeInTheDocument();
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
    expect(queryByText('Batch size')).not.toBeInTheDocument();
    expect(queryByText('Batch time window')).not.toBeInTheDocument();
    expect(queryByText('Batch volume')).not.toBeInTheDocument();
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
    const batchVolumeRow = getByText('Sending delay').closest('tr')!;
    expect(
      within(batchVolumeRow).getByText('200 milliseconds'),
    ).toBeInTheDocument();
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
    expect(queryByText('Rate limit')).not.toBeInTheDocument();
    expect(queryByText('Sending delay')).not.toBeInTheDocument();
    expect(queryByText('Retry backoff multiplier')).not.toBeInTheDocument();
    expect(queryByText('Retry backoff max interval')).not.toBeInTheDocument();
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
    const batchVolumeRow = getByText('Message TTL').closest('tr')!;
    expect(within(batchVolumeRow).getByText('60 seconds')).toBeInTheDocument();
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
    const batchVolumeRow = getByText('Request timeout').closest('tr')!;
    expect(
      within(batchVolumeRow).getByText('1000 milliseconds'),
    ).toBeInTheDocument();
  });

  it.each([
    ['trackingOff', 'No tracking'],
    ['discardedOnly', 'Track message discarding only'],
    ['trackingAll', 'Track everything'],
    ['someUnknownValue', 'Unknown'],
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
      const trackingRow = getByText('Message delivery tracking').closest('tr')!;
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
      const retry4xxRow = getByText('Retry on 4xx status').closest('tr')!;
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
    const retryBackoffRow = getByText('Retry backoff').closest('tr')!;
    expect(
      within(retryBackoffRow).getByText('150 milliseconds'),
    ).toBeInTheDocument();
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
      'Retry backoff multiplier',
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
      'Retry backoff max interval',
    ).closest('tr')!;
    expect(within(retryBackoffMaxIntervalRow).getByText('400 seconds'));
  });

  it.each([
    [Severity.CRITICAL, 'CRITICAL'],
    [Severity.IMPORTANT, 'IMPORTANT'],
    [Severity.NON_IMPORTANT, 'NON_IMPORTANT'],
  ])(
    'should render subscription monitoring severity (%s)',
    (severity, severityName) => {
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
      const severityRow = getByText('Monitoring severity').closest('tr')!;
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
    const reactionRow = getByText('Monitoring reaction').closest('tr')!;
    expect(within(reactionRow).getByText('foo')).toBeInTheDocument();
  });

  it.each([true, false])(
    'should render subscription http/2 configuration flag (%s)',
    (http2Enabled) => {
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
      const http2Row = getByText('Deliver using http/2').closest('tr')!;
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
        'Attach subscription identity headers',
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
    (autoDeleteWithTopicEnabled) => {
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
      const autoDeleteRow = getByText('Automatically remove').closest('tr')!;
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
    const createdAtRow = getByText('Creation date').closest('tr')!;
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
    const modifiedAtRow = getByText('Modification date').closest('tr')!;
    expect(
      within(modifiedAtRow).getByText('2023-02-18 15:54:57'),
    ).toBeInTheDocument();
  });
});
