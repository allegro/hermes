package pl.allegro.tech.hermes.management.domain.retransmit;

import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;

public class DefaultRetransmissionMonitoringUrlProvider
    implements RetransmissionMonitoringUrlProvider {

  @Override
  public String getLogsUrl(OfflineRetransmissionTask task) {
    return ("https://web.logger-dev.qxlint/app/kibana#/discover/899e0110-c571-11e8-bad6-976158382a4a?_g=(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:now-1h,mode:quick,to:now))&_a=(columns:!(message),filters:!(),index:'88e2c760-c571-11e8-bad6-976158382a4a',interval:auto,query:(language:kuery,query:%22{taskId}%22),sort:!('@timestamp',desc))"
        .replace("{taskId}", task.getTaskId()));
  }

  @Override
  public String getMetricsUrl(OfflineRetransmissionTask task) {
    return "https://console.cloud.google.com/monitoring/dashboards/builder/7df4acb0-872a-4c11-9f41-66df10881dcc;startTime=2025-04-24T13:37:13.451Z;endTime=2025-04-24T13:59:53.731Z;filters=type:rlabel,key:job_name,val:retransmission-{taskId}?project=sc-638-bigquery2hermes-dev&dashboardBuilderState=%257B%2522editModeEnabled%2522:false%257D&inv=1&invt=Abvtkg&pageState=(%22eventTypes%22:(%22selected%22:%5B%22CLOUD_ALERTING_ALERT%22%5D))"
        .replace("{taskId}", task.getTaskId());
  }

  @Override
  public String getJobDetailsUrl(OfflineRetransmissionTask task) {
    return "https://console.cloud.google.com/dataflow/jobs?inv=1&invt=Abvtdw&project=sc-638-bigquery2hermes-dev";
  }
}
