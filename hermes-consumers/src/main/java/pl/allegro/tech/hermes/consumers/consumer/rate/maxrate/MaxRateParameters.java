package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.time.Duration;

public interface MaxRateParameters {

  Duration getBalanceInterval();

  Duration getUpdateInterval();

  int getHistorySize();

  double getBusyTolerance();

  double getMinMaxRate();

  double getMinAllowedChangePercent();

  double getMinSignificantUpdatePercent();
}
