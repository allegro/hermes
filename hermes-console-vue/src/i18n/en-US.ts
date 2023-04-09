const en_US = {
  topicView: {
    header: {
      topic: 'TOPIC',
      owner: 'Owner:',
      actions: {
        edit: 'Edit',
        clone: 'Clone',
        offlineRetransmission: 'Offline retransmission',
        remove: 'Remove',
      },
    },
    metrics: {
      header: 'Metrics',
      rate: 'Rate',
      deliveryRate: 'Delivery rate',
      published: 'Published',
      latency: 'Latency',
      messageSize: 'Message size',
    },
    properties: {
      header: 'Properties',
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
          'Specifies the strength of guarantees that acknowledged message was indeed persisted. In ' +
          '"Leader" mode ACK is required only from topic leader, which is fast and gives 99.99999% guarantee. It might ' +
          'be not enough when cluster is unstable. "All" mode means message needs to be saved on all replicas before ' +
          'sending ACK, which is quite slow but gives 100% guarantee that message has been persisted.',
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
    },
  },
};

export default en_US;
