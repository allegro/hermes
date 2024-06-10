const messages = {
  fields: {
    name: {
      label: 'Name',
      placeholder: 'Name of the Topic',
    },
    description: {
      label: 'Description',
      placeholder: 'Who and why publishes?',
    },
    ownerSource: {
      label: 'Owner source',
    },
    owner: {
      label: 'Owner',
    },
    auth: {
      enabled: 'Authorisation enabled',
      publishers: {
        label: 'Authorised publishers',
        placeholder:
          'who is allowed to publish on this topic? (comma seperated)',
      },
      unauthenticatedAccessEnabled: 'Allow unauthenticated access',
    },
    trackingEnabled: 'Tracking enabled',
    restrictSubscribing: 'Restrict subscribing',
    retentionTime: {
      unit: 'Retention unit',
      duration: 'Retention time',
      infinite: 'Keep forever',
      days: 'DAYS',
    },
    ack: 'Kafka ACK level',
    ackHelpTitle: 'ACK level is very important',
    ackHelpText:
      'Set ACK level according to your durability and latency requirements, see: ',
    ackHelpLink:
      'https://hermes-pubsub.readthedocs.io/en/latest/user/publishing/#acknowledgment-level',
    ackHelpLinkDescription: 'ACK docs.',
    contentType: 'Content type',
    maxMessageSize: {
      label: 'Max message size',
      suffix: 'bytes',
    },
    storeOffline: 'Store offline',
    schema: 'Avro schema',
    beautify: 'Beautify',
  },
  warnings: {
    highRequestTimeout: {
      title: 'High request timeout',
      text: 'Please consider lower value to comply with the fail-fast principle. In some cases it is better to rethink the design of a subscriber rather than increase the timeout.',
    },
    trackingEnabled: {
      title: 'Warning: Tracking all messages enabled',
      text: 'Please chose this option only when necessary. Mainly this is for debugging problems with subscription. Remember to disable this mode after the problem is solved.',
    },
  },
  info: {
    avro: {
      title: 'Schema must contain an additional field for internal use:',
    },
  },
  actions: {
    create: 'Create topic',
    update: 'Update topic',
    cancel: 'Cancel',
    import: 'Import from file',
  },
};

export default messages;
