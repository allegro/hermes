package pl.allegro.tech.hermes.management.domain.retransmit;

import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;

public class DefaultRetransmissionMonitoringUrlProvider
    implements RetransmissionMonitoringUrlProvider {

  @Override
  public String getLogsUrl(OfflineRetransmissionTask task) {
    return "https://kibana.com/?query=%s".formatted(task.getTaskId());
  }

  @Override
  public String getMetricsUrl(OfflineRetransmissionTask task) {
    return "https://monitoring.com?query=%s".formatted(task.getTaskId());
  }

  @Override
  public String getJobDetailsUrl(OfflineRetransmissionTask task) {
    return "https://job-details.com?query=%s".formatted(task.getTaskId());
  }
}
