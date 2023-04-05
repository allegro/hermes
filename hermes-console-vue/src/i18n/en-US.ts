const en_US = {
  consistency: {
    connectionError: {
      title: 'Connection error',
      text: 'Could not fetch information about consistency',
    },
    breadcrumbs: {
      home: 'home',
      title: 'consistency',
    },
    inconsistentTopics: {
      noTopics: 'No inconsistent topics found',
      appliedFilter: '(applied filter: “{filter}”)',
      heading: 'Topics existing on kafka cluster but not present in hermes',
      actions: {
        delete: 'Remove',
        search: 'Search inconsistent topics...',
      },
      listing: {
        index: '#',
        name: 'Topic Name',
      },
    },
  },
  consumerGroups: {
    connectionError: {
      title: 'Connection error',
      text: 'Could not fetch information about consumerGroups',
    },
    title: 'Consumer Groups',
    groupId: 'GroupId: ',
    breadcrumbs: {
      home: 'home',
      groups: 'groups',
      title: 'consumer groups',
    },
    listing: {
      type: 'Type',
      partition: 'Partition',
      currentOffset: 'Current Offset',
      endOffset: 'End Offset',
      lag: 'Lag',
      host: 'Host: ',
    },
  },
  constraints: {
    connectionError: {
      title: 'Connection error',
      text: 'Could not fetch information about constraints',
    },
    topicConstraints: {
      heading: 'Topic constraints',
      actions: {
        create: 'Add constraint',
        search: 'Search topic constraints...',
      },
    },
    subscriptionConstraints: {
      heading: 'Subscription constraints',
      actions: {
        create: 'Add constraint',
        search: 'Search subscription constraints...',
      },
    },
    constraintsListing: {
      index: '#',
      name: 'Group name',
      noGroups: 'No groups found',
      topicsChip: 'topics: ',
      appliedFilter: '(applied filter: “{filter}”)',
    },
    breadcrumbs: {
      home: 'home',
      title: 'constraints',
    },
    listing: {
      index: '#',
      name: 'Name',
      noConstraints: 'No constraints found',
      consumersNumberChip: 'consumers number: ',
      appliedFilter: '(applied filter: “{filter}”)',
    },
  },
  readiness: {
    title: 'Datacenters Readiness',
    turnOn: 'Turn on',
    turnOff: 'Turn off',
    index: '#',
    datacenter: 'Datacenter',
    isReady: 'Is ready',
    control: 'Control',
    breadcrumbs: {
      home: 'home',
      title: 'readiness',
    },
    connectionError: {
      title: 'Connection error',
      text: 'Could not fetch information about datacenters readiness',
    },
  },
  subscription: {
    connectionError: {
      title: 'Connection error',
      text: 'Could not fetch {subscriptionId} subscription details',
    },
    filtersCard: {
      title: 'Subscription message filters',
      index: '#',
      type: 'Type',
      path: 'Path',
      matcher: 'Matcher',
      matchingStrategy: 'Matching strategy',
      debug: 'Debug',
    },
    headersCard: {
      title: 'Fixed HTTP headers',
      index: '#',
      name: 'Name',
      value: 'Value',
    },
    healthProblemsAlerts: {
      lagging: {
        title: 'Subscription lagging',
        text:
          'Subscription lag is growing. Examine output rate and service ' +
          'response codes, looks like it is not consuming at full speed.',
      },
      malfunctioning: {
        title: 'Subscription malfunctioning',
        text:
          'Consuming service returns a lot of 5xx codes. Looks like it ' +
          "might be malfunctioning or doesn't know how to handle messages. " +
          'Take a look at "Last undelivered message" for more information.',
      },
      malformedMessages: {
        title: 'Subscription receiving malformed messages',
        text:
          'Consuming service returns a lot of 4xx codes. Maybe you are ' +
          'receiving some malformed messages? If this is normal behavior, ' +
          'switch Retry on 4xx status flag to false. This way Hermes will ' +
          'not try to resend malformed messages, reducing traffic.',
      },
      timingOut: {
        title: 'Subscription timing out',
        text:
          'Consuming service times out a lot. Hermes times out after ' +
          '1 second, if you are not able to process message during this ' +
          'time, connection is reset and delivery fails.',
      },
      unreachable: {
        title: 'Consuming service unreachable',
        text:
          'Unable to connect to consuming service instances. It is either' +
          'network issue or your service instance is down.',
      },
    },
    lastUndeliveredMessage: {
      title: 'Last undelivered message',
      time: 'Time',
      reason: 'Reason',
      message: 'Message',
    },
    manageMessagesCard: {
      title: 'Manage subscription messages',
    },
    metricsCard: {
      title: 'Subscription metrics',
      deliveryRate: 'Delivery rate',
      subscriberLatency: 'Subscriber latency',
      delivered: 'Delivered',
      discarded: 'Discarded',
      lag: 'Lag',
      outputRate: 'Output rate',
      tooltips: {
        subscriberLatency:
          'Latency of acknowledging messages by subscribing service as ' +
          'measured by Hermes.',
        lag:
          'Total number of events waiting to be delivered. Each subscription ' +
          'has a "natural" lag, which depends on production rate.',
        outputRate:
          'Maximum sending rate calculated based on receiving service ' +
          'performance. For well-performing service output rate should be ' +
          'equal to rate limit.',
      },
    },
    propertiesCard: {
      title: 'Properties',
      contentType: 'Content type',
      deliveryType: 'Delivery type',
      mode: 'Mode',
      rateLimit: 'Rate limit',
      batchSize: 'Batch size',
      batchTime: 'Batch time window',
      batchVolume: 'Batch volume',
      requestTimeout: 'Request timeout',
      sendingDelay: 'Sending delay',
      messageTtl: 'Message TTL',
      trackingMode: 'Message delivery tracking',
      trackingOff: 'No tracking',
      discardedOnly: 'Track message discarding only',
      trackingAll: 'Track everything',
      unknown: 'Unknown',
      retryClientErrors: 'Retry on 4xx status',
      retryBackoff: 'Retry backoff',
      backoffMultiplier: 'Retry backoff multiplier',
      backoffMaxInterval: 'Retry backoff max interval',
      monitoringSeverity: 'Monitoring severity',
      monitoringReaction: 'Monitoring reaction',
      http2: 'Deliver using http/2',
      subscriptionIdentityHeaders: 'Attach subscription identity headers',
      autoDeleteWithTopic: 'Automatically remove',
      createdAt: 'Creation date',
      modifiedAt: 'Modification date',
      tooltips: {
        deliveryType:
          'Hermes can deliver messages in SERIAL (one message at a time) or ' +
          'in BATCH (group of messages at a time).',
        mode:
          'Hermes can deliver messages in ANYCAST (to one of subscribed ' +
          'hosts) or in BROADCAST (to all subscribed hosts) mode.',
        rateLimit:
          'Maximum rate defined by user (per data center). Maximum rate ' +
          'calculated by algorithm can be observed in "Output rate" metric.',
        batchSize: 'Desired number of messages in a single batch.',
        batchTime:
          'Max time between arrival of first message to batch delivery attempt.',
        batchVolume: 'Desired number of bytes in single batch.',
        requestTimeout: 'HTTP client request timeout in milliseconds.',
        sendingDelay:
          'Amount of time in ms after which an event will be send. Useful if ' +
          'events from two topics are sent at the same time and you want to ' +
          'increase chance that events from one topic will be deliver after ' +
          'events from other topic.',
        messageTtl:
          'Amount of time a message can be held in sending queue and retried. ' +
          'If message will not be delivered during this time, it will be ' +
          'discarded.',
        retryClientErrors:
          'If false, message will not be retried when service responds with ' +
          '4xx status (i.e. Bad Request).',
        retryBackoff:
          'Minimum amount of time between consecutive message retries.',
        backoffMultiplier:
          'Delay multiplier between consecutive send attempts of failed requests',
        backoffMaxInterval:
          'Maximum value of delay backoff when using exponential calculation',
        monitoringSeverity:
          "How important should be the subscription's health for the monitoring.",
        http2: 'If true Hermes will deliver messages using http/2 protocol.',
        subscriptionIdentityHeaders:
          'If true Hermes will attach HTTP headers with subscription identity.',
        autoDeleteWithTopic:
          'When the associated topic is deleted, Hermes will delete the ' +
          'subscription automatically.',
      },
    },
    serviceResponseMetrics: {
      title: 'Service response metrics',
      '2xx': '2xx',
      '4xx': '4xx',
      '5xx': '5xx',
      networkTimeouts: 'Network timeouts',
      otherNetworkErrors: 'Other network errors',
    },
    showEventTrace: {
      title: 'Show event trace',
    },
    subscriptionBreadcrumbs: {
      home: 'home',
      groups: 'groups',
    },
    subscriptionMetadata: {
      subscription: 'Subscription',
      owners: 'Owners',
      unauthorizedTooltip: 'Sign in to edit the subscription',
      actions: {
        diagnostics: 'Diagnostics',
        suspend: 'Suspend',
        activate: 'Activate',
        edit: 'Edit',
        clone: 'Clone',
        remove: 'Remove',
      },
    },
    undeliveredMessagesCard: {
      title: 'Last 100 undelivered messages',
      index: '#',
      messageId: 'MessageId',
      status: 'Status',
      reason: 'Reason',
      timestamp: 'Timestamp',
    },
  },
};

export default en_US;
