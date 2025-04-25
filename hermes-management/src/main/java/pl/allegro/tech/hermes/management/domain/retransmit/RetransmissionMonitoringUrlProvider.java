package pl.allegro.tech.hermes.management.domain.retransmit;

import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;

interface RetransmissionMonitoringUrlProvider {
  String getLogsUrl(OfflineRetransmissionTask task);

  String getMetricsUrl(OfflineRetransmissionTask task);

  String getJobDetailsUrl(OfflineRetransmissionTask task);
}
