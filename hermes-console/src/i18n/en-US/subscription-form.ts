const messages = {
  fields: {
    name: {
      label: 'Name',
      placeholder: 'Name of the subscription',
    },
    endpoint: {
      label: 'Endpoint',
      placeholder: 'Where to send messages',
    },
    description: {
      label: 'Description',
      placeholder: 'Who and why subscribes?',
    },
    ownerSource: {
      label: 'Owner source',
    },
    owner: {
      label: 'Owner',
    },
    deliveryType: {
      label: 'Delivery type',
    },
    contentType: {
      label: 'Content type',
    },
    mode: {
      label: 'Mode',
    },
    rateLimit: {
      label: 'Rate limit',
      placeholder: 'Limit of messages sent by Hermes',
      suffix: 'messages/second',
    },
    batchSize: {
      label: 'Batch size',
      placeholder: 'Desired number of messages in a single batch',
      suffix: 'messages',
    },
    batchTime: {
      label: 'Batch time window',
      placeholder:
        'Max time between arrival of first message to batch delivery attempt',
      suffix: 'milliseconds',
    },
    batchVolume: {
      label: 'Batch volume',
      placeholder: 'Desired number of bytes in single batch',
      suffix: 'bytes',
    },
    requestTimeout: {
      label: 'Request timeout',
      placeholder: 'Max time for processing message by the subscriber',
      suffix: 'milliseconds',
    },
    sendingDelay: {
      label: 'Sending delay',
      placeholder: 'Delay after which an event will be send',
      suffix: 'milliseconds',
    },
    inflightMessageTTL: {
      label: 'Inflight message TTL',
      placeholder:
        'Time when message can be resent to endpoint after failed sending attempts',
      suffix: 'seconds',
    },
    inflightMessagesCount: {
      label: 'Inflight messages count',
      placeholder:
        'How many messages can be in inflight state at the same time',
    },
    retryOn4xx: {
      label: 'Retry on http 4xx status',
    },
    retryBackoff: {
      label: 'Retry backoff',
      placeholder: 'Delay between send attempts of failed requests',
      suffix: 'milliseconds',
    },
    retryBackoffMultiplier: {
      label: 'Retry backoff multiplier',
      placeholder:
        'Delay multiplier between consecutive send attempts of failed requests',
    },
    backoffMaxIntervalInSec: {
      label: 'Retry backoff max interval',
      placeholder:
        'Max delay between consecutive send attempts of failed requests',
      suffix: 'seconds',
    },
    messageDeliveryTrackingMode: {
      label: 'Message delivery tracking mode',
    },
    monitoringSeverity: {
      label: 'Monitoring severity',
    },
    monitoringReaction: {
      label: 'Monitoring reaction',
      placeholder:
        'how to react when the subscription becomes unhealthy (e.g. team name or Pager Duty ID)',
    },
    deliverUsingHttp2: {
      label: 'Deliver using http/2',
    },
    attachSubscriptionIdentityHeaders: {
      label: 'Attach subscription identity headers',
    },
    deleteSubscriptionAutomatically: {
      label: 'Delete the subscription automatically',
    },
  },
  sections: {
    filters: {
      heading: 'Message content filters',
    },
  },
  warnings: {
    highRequestTimeout: {
      title: 'High request timeout',
      text: 'Please consider lower value to comply with the fail-fast principle. In some cases it is better to rethink the design of a subscriber rather than increase the timeout.',
    },
    trackingMode: {
      title: 'Warning: Tracking all messages enabled',
      text: 'Please chose this option only when necessary. Mainly this is for debugging problems with subscription. Remember to disable this mode after the problem is solved.',
    },
    adminForm: {
      title: 'Warning: Admin form enabled.',
      text: 'New fields in the form have been revealed (they are followed by a warning sign). The form will be submitted regardless of the validation of the fields.',
    },
  },
  actions: {
    create: 'Create subscription',
    update: 'Update subscription',
    cancel: 'Cancel',
    import: 'Import from file',
  },
};

export default messages;
