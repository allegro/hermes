import subscriptionForm from '@/i18n/en-US/subscription-form';
import topicForm from '@/i18n/en-US/topic-form';

const en_US = {
  homeView: {
    links: {
      console: 'Console',
      favoriteTopics: 'Favorite topics',
      favoriteSubscriptions: 'Favorite subs',
      runtime: 'Runtime',
      statistics: 'Stats',
      search: 'Search',
      documentation: 'Docs',
      costs: 'Costs',
      adminTools: 'Admin tools',
    },
  },
  header: {
    signIn: 'Sign in',
    logout: 'Logout',
  },
  favorites: {
    breadcrumbs: {
      home: 'home',
      topics: 'favorite topics',
      subscriptions: 'favorite subscriptions',
    },
    topics: {
      heading: 'Favorite topics',
      actions: {
        search: 'Search favorite topics...',
      },
    },
    subscriptions: {
      index: '#',
      name: 'Qualified subscription name',
      appliedFilter: '(applied filter: “{filter}”)',
      heading: 'Favorite subscriptions',
      actions: {
        search: 'Search favorite subscriptions...',
      },
    },
  },
  confirmationDialog: {
    confirm: 'Confirm',
    cancel: 'Cancel',
    confirmText: "Type 'prod' to confirm action.",
  },
  consistency: {
    connectionError: {
      title: 'Connection error',
      text: 'Could not fetch information about consistency',
    },
    breadcrumbs: {
      home: 'home',
      title: 'consistency',
    },
    inconsistentGroups: {
      heading: 'Inconsistent groups',
      noGroups: 'No inconsistent groups found',
      appliedFilter: '(applied filter: “{filter}”)',
      actions: {
        search: 'Search inconsistent groups...',
        check: 'Check consistency',
      },
      listing: {
        index: '#',
        name: 'Group',
      },
    },
    inconsistentGroup: {
      title: 'Group: {groupId}',
      listing: {
        index: '#',
        name: 'Topic',
        title: 'Inconsistent topics',
      },
      noTopics: 'No inconsistent topics',
      inconsistentTopic: {
        title: 'Topic: {topicId}',
        listing: {
          index: '#',
          name: 'Subscription',
        },
        noSubscriptions: 'No inconsistent subscriptions',
        inconsistentSubscriptions: 'Inconsistent subscriptions',
      },
      metadata: {
        consistent: 'Metadata are consistent',
        inconsistent: 'Inconsistent metadata',
      },
    },
    inconsistentTopics: {
      confirmationDialog: {
        remove: {
          title: 'Confirm topic deletion',
          text: 'Are you sure you want to delete topic {topicToDelete}',
        },
      },
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
    createForm: {
      createSubscriptionTitle: 'Create constraints for subscription',
      createTopicTitle: 'Create constraints for topic',
      topicName: 'Topic name',
      subscriptionName: 'Subscription name',
      consumersNumber: 'Consumers count',
      save: 'Save',
      cancel: 'Cancel',
    },
    editForm: {
      title: 'Edit constraints for: “{resourceId}”',
      consumersNumber: 'Consumers count',
      save: 'Save',
      remove: 'Remove',
      cancel: 'Cancel',
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
  stats: {
    connectionError: {
      title: 'Connection error',
      text: 'Could not fetch stats',
    },
    title: 'Statistics',
    topics: 'Topics',
    subscriptions: 'Subscriptions',
    total: 'Total',
    ackAll: 'Ack ALL',
    trackingEnabled: 'Tracking Enabled',
  },
  readiness: {
    confirmationDialog: {
      switch: {
        title: 'Confirm readiness switch',
        text: 'Are you sure you want to {switchAction} datacenter {dcToSwitch}?',
      },
    },
    title: 'Datacenters Readiness',
    turnOn: 'turn on',
    turnOff: 'turn off',
    index: '#',
    datacenter: 'Datacenter',
    status: 'Status',
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
  groups: {
    actions: {
      createTopic: 'Create Topic',
      remove: 'Remove',
      create: 'New Group',
      search: 'search…',
    },
    confirmationDialog: {
      remove: {
        title: 'Confirm group deletion',
        text: 'Are you sure you want to delete group {groupId}',
      },
    },
    connectionError: {
      title: 'Connection error',
      text: 'Could not fetch topic groups',
    },
    groupBreadcrumbs: {
      home: 'home',
      groups: 'groups',
    },
    groupForm: {
      cancel: 'Cancel',
      edu:
        'When creating a group please make sure that it complies with ' +
        'general group standards and naming conventions. Ensure that ' +
        'a group that meets your requirements does not already exist.',
      groupName: 'Group name',
      save: 'Save',
      createTitle: 'Create group',
      editTitle: 'Edit group',
      validation: {
        groupName: 'Group name must not be empty',
      },
    },
    groupListing: {
      index: '#',
      name: 'Group name',
      noGroups: 'No groups found',
      topicsChip: 'topics: ',
      appliedFilter: '(applied filter: “{filter}”)',
    },
    groupTopicsListing: {
      index: '#',
      name: 'Topic name',
      noTopics: 'No topics found',
      appliedFilter: '(applied filter: “{filter}”)',
    },
    heading: 'Groups',
  },
  groupTopics: {
    title: 'Group',
    groupTopicsBreadcrumbs: {
      home: 'home',
      groups: 'groups',
    },
  },
  topicView: {
    confirmationDialog: {
      remove: {
        title: 'Confirm topic deletion',
        text: 'Are you sure you want to delete topic {topicName}',
      },
    },
    header: {
      editTopic: 'Edit topic: {topicName}',
      unauthorizedTooltip: "You don't have permissions to manage this topic",
      topic: 'TOPIC',
      owner: 'OWNER:',
      actions: {
        edit: 'Edit',
        export: 'Export',
        offlineRetransmission: 'Offline retransmission',
        remove: 'Remove',
        copyName: 'Copy topic name',
        addToFavorites: 'Add topic to favorites',
        removeFromFavorites: 'Remove topic from favorites',
      },
    },
    metrics: {
      dashboard: 'DASHBOARD',
      title: 'Metrics',
      rate: 'Rate',
      deliveryRate: 'Delivery rate',
      published: 'Published',
      latency: 'Latency',
      messageSize: 'Message size',
    },
    properties: {
      title: 'Properties',
      contentType: 'Content type',
      labels: 'Labels',
      acknowledgement: 'Acknowledgement',
      retentionTime: 'Retention time',
      trackingEnabled: 'Tracking enabled',
      maxMessageSize: 'Max message size',
      schemaIdAwareSerializationEnabled: 'SchemaId serialization enabled',
      authorizationEnabled: 'Authorization enabled',
      authorizedPublishers: 'Authorized publishers',
      allowUnauthenticatedAccess: 'Allow unauthenticated access',
      restrictSubscribing: 'Restrict subscribing',
      storeOffline: 'Store offline',
      offlineRetention: 'Offline retention',
      creationDate: 'Creation date',
      modificationDate: 'Modification date',
      tooltips: {
        acknowledgement:
          'Specifies the strength of guarantees that acknowledged message was indeed persisted. ' +
          'With `ACK leader` message writes are replicated asynchronously, thus the acknowledgment latency will be low. However, message write may be lost when there is a topic leadership change - e.g. due to rebalance or broker restart. ' +
          'With `ACK all` messages writes are synchronously replicated to replicas. Write acknowledgement latency will be much higher than with leader ACK,' +
          ' it will also have higher variance due to tail latency. However, messages will be persisted as long as the whole replica set does not go down simultaneously.',
        retentionTime:
          'For how many hours/days message is available for subscribers after being published.',
        authorizedPublishers:
          'When authorisation is enabled, only authenticated services are allowed to publish on this topic.',
        allowUnauthenticatedAccess:
          'Allowing unauthenticated access should be enabled only when migrating topic to ' +
          'authorised, so both authenticated and unauthenticated clients are allowed to publish simultaneously.',
        restrictSubscribing:
          'When subscribing is restricted, only owner of this topic can create new subscriptions.',
        storeOffline:
          'Should data from this topic be stored in offline storage (e.g. HDFS).',
        offlineRetention:
          'For how long should this topic be stored in offline storage.',
      },
      ackText: {
        all: 'All brokers',
        leader: 'Leader only',
        none: 'None',
      },
      authorizedPublishersNotSet: 'Not set',
    },
    messagesPreview: {
      title: 'Topic messages preview',
    },
    offlineClients: {
      title: 'Offline clients',
    },
    schema: {
      copy: 'Copy to clipboard',
      default: 'Default',
      showRawSchema: 'Show raw schema',
      title: 'Message schema',
    },
    subscriptions: {
      title: 'Subscriptions',
      create: 'Create subscription',
      search: 'Search...',
    },
    errorMessage: {
      topicFetchFailed: 'Could not fetch {topicName} topic details',
    },
  },
  subscription: {
    confirmationDialog: {
      remove: {
        title: 'Confirm subscription deletion',
        text: 'Are you sure you want to delete subscription {subscriptionId}',
      },
      suspend: {
        title: 'Confirm subscription suspension',
        text: 'Are you sure you want to suspend subscription {subscriptionId}',
      },
      activate: {
        title: 'Confirm subscription activation',
        text: 'Are you sure you want to activate subscription {subscriptionId}',
      },
      retransmit: {
        title: 'Confirm subscription messages retransmission',
        text: 'This action will re-send all messages on subscription “{subscriptionFqn}“ from {fromDate} (UTC). Do you want to continue?',
      },
      skipAllMessages: {
        title: 'Confirm skipping all offsets',
        text: 'This action will skip all undelivered messages for subscription: “{subscriptionFqn}“ . Do you want to continue?',
      },
    },
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
      retransmitTitle: 'Retransmit messages from the past',
      retransmitStartTimestampLabel: 'Retransmit since timestamp (UTC)',
      retransmitButton: 'Retransmit',
      skipAllMessagesTitle: 'Skip all messages',
      skipAllMessagesButton: 'Skip messages',
    },
    metricsCard: {
      title: 'Subscription metrics',
      deliveryRate: 'Delivery rate',
      dashboard: 'DASHBOARD',
      subscriberLatency: 'Subscriber latency',
      delivered: 'Delivered',
      discarded: 'Discarded',
      timeouts: 'Timeouts',
      otherErrors: 'Other errors',
      codes2xx: 'Codes 2xx',
      codes4xx: 'Codes 4xx',
      codes5xx: 'Codes 5xx',
      retries: 'Retries',
      lag: 'Lag',
      tooltips: {
        subscriberLatency:
          'Latency of acknowledging messages by subscribing service as ' +
          'measured by Hermes.',
        lag:
          'Total number of events waiting to be delivered. Each subscription ' +
          'has a "natural" lag, which depends on production rate.',
        retries:
          'Total number of message sending retries. Retrying messages significantly reduces the rate on subscriptions.',
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
      backoffMaxIntervalInSec: 'Retry backoff max interval',
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
      editSubscription: 'Edit subscription {subscriptionName}',
      subscription: 'Subscription',
      owners: 'OWNER:',
      unauthorizedTooltip:
        "You don't have permissions to manage this subscription",
      actions: {
        diagnostics: 'Diagnostics',
        suspend: 'Suspend',
        activate: 'Activate',
        edit: 'Edit',
        export: 'Export',
        remove: 'Remove',
        copyName: 'Copy subscription name',
        addToFavorites: 'Add subscription to favorites',
        removeFromFavorites: 'Remove subscription from favorites',
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
    moveOffsets: {
      tooltip: 'Move subscription offsets to the end',
      button: 'MOVE OFFSETS',
    },
  },
  search: {
    collection: {
      topics: 'topics',
      subscriptions: 'subscriptions',
    },
    filter: {
      name: 'by name',
      endpoint: 'by endpoint',
      owner: 'by owner',
    },
    pattern: 'regex pattern',
    connectionError: {
      title: 'Connection error',
      text: 'Could not fetch query results',
    },
    results: {
      topic: {
        name: 'name',
        owner: 'owner',
        noTopics: 'No topics found',
      },
      subscription: {
        name: 'name',
        status: 'status',
        owner: 'owner',
        endpoint: 'endpoint',
        noSubscriptions: 'No subscriptions found',
      },
    },
  },
  offlineRetransmission: {
    title: 'Offline retransmission',
    subtitle:
      'Offline retransmission allows retransmitting events from GCP (BigQuery) to Hermes.',
    targetTopic: 'Target topic',
    startTimestamp: 'Start timestamp (UTC)',
    endTimestamp: 'End timestamp (UTC)',
  },
  notifications: {
    dashboardUrl: {
      error: 'Failed to fetch dashboard url',
    },
    copy: {
      success: 'Successfully copied content',
      error: 'Failed to copy content',
    },
    unknownError: 'Unknown error occurred',
    form: {
      beautifyError: 'Failed to beatify schema',
      validationError: 'Some fields are not valid',
      parseError: 'Error parsing form data',
      fetchTopicContentTypeError: 'Error fetching topic content type',
    },
    subscriptionOffsets: {
      move: {
        success: 'Moved offsets for subscription {subscriptionName}',
        failure: 'Failed to move offsets for subscription {subscriptionName}',
      },
    },
    readiness: {
      switch: {
        success: 'Successfully switched datacenter {datacenter} readiness',
        failure: "Couldn't switch datacenter {datacenter} readiness",
      },
    },
    roles: {
      fetch: {
        failure:
          'Fetching user roles failed. Some options might not be visible.',
      },
    },
    group: {
      delete: {
        success: 'Group {groupId} successfully deleted',
        failure: "Couldn't delete group {groupId}",
      },
      create: {
        success: 'Group {groupId} successfully created',
        failure: "Couldn't create group {groupId}",
      },
    },
    topic: {
      edit: {
        success: 'Topic {topicName} successfully updated',
        failure: "Couldn't update topic {topicName}",
      },
      create: {
        success: 'Topic {topicName} successfully created',
        failure: "Couldn't create topic {topicName}",
      },
      delete: {
        success: 'Topic {topicName} successfully deleted',
        failure: "Couldn't delete topic {topicName}",
      },
    },
    inconsistentTopic: {
      delete: {
        success: 'Topic {topic} successfully deleted',
        failure: "Couldn't delete topic {topic}",
      },
    },
    subscription: {
      create: {
        success: 'Subscription {subscriptionName} successfully created',
        failure: "Couldn't create subscription {subscriptionName}",
      },
      edit: {
        success: 'Subscription {subscriptionName} successfully updated',
        failure: "Couldn't update subscription {subscriptionName}",
      },
      delete: {
        success: 'Subscription {subscriptionName} successfully deleted',
        failure: "Couldn't delete subscription {subscriptionName}",
      },
      suspend: {
        success: 'Subscription {subscriptionName} successfully suspended',
        failure: "Couldn't suspend subscription {subscriptionName}",
      },
      activate: {
        success: 'Subscription {subscriptionName} successfully activated',
        failure: "Couldn't activate subscription {subscriptionName}",
      },
      retransmit: {
        success:
          'Successfully retransmitted messages on subscription: “{subscriptionName}“',
        failure:
          "Couldn't retransmit messages on subscription: “{subscriptionName}“",
      },
      skipAllMessages: {
        success:
          'Successfully skipped all messages on subscription: “{subscriptionName}“',
        failure:
          "Couldn't skip all messages on subscription: “{subscriptionName}“",
      },
    },
    constraints: {
      topic: {
        created: {
          success: 'Successfully upserted constraint for topic: “{topicName}“',
          failure: 'Failed to upsert constraint for topic: “{topicName}“',
        },
        deleted: {
          success: 'Successfully deleted constraint for topic: “{topicName}“',
          failure: 'Failed to delete constraint for topic: “{topicName}“',
        },
      },
      subscription: {
        created: {
          success:
            'Successfully upserted constraint for subscription: “{subscriptionFqn}“',
          failure:
            'Failed to upsert constraint for subscription: “{subscriptionFqn}“',
        },
        deleted: {
          success:
            'Successfully deleted constraint for subscription: “{subscriptionFqn}“',
          failure:
            'Failed to delete constraint for subscription: “{subscriptionFqn}“',
        },
      },
    },
    offlineRetransmission: {
      create: {
        success:
          'Successfully created retransmission task from topic: “{sourceTopic}“ to topic: “{targetTopic}“',
        failure:
          'Failed to create retransmission task from topic: “{sourceTopic}“ to topic: “{targetTopic}“',
      },
    },
    subscriptionFiltersDebug: {
      fetchTopicContentType: {
        failure:
          'Failed to fetch content type for topic: “{topicName}“. Filter debugging not possible.',
      },
      verification: {
        failure: 'Failed to verify filters',
      },
    },
  },
  subscriptionForm,
  topicForm,
  filterDebug: {
    title: 'Debug subscription filters',
    cancelButton: 'Cancel',
    saveButton: 'Update subscription filters',
    verifyButton: 'Verify',
    placeholder: 'Paste your message here...',
    debugButton: 'Debug filters',
    matched: 'Matched',
    notMatched: 'Not Matched',
    error: 'Error',
  },
  costsCard: {
    title: 'Costs',
    detailsButton: 'DASHBOARD',
  },
};

export default en_US;
