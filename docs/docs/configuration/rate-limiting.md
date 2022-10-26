# Consumers rate limiter

[Subscribing user guide](../user/subscribing.md) presents an overview of Consumers adaptive rate limiting algorithm.
This section describes the algorithm in detail along with all configuration options that can be used
to fine-tune it.

Measurement window for each subscription is *LIMITER_PERIOD* seconds. Each *LIMITER_PERIOD* seconds recalculation of
output rate is triggered. There are three algorithm phases (modes), by default subscription is started in **normal mode**:

* if there were no more than *SPEEDUP_TOLERANCE*% of failures, increase sending speed by *CONVERGENCE_FACTOR*%
* if there was more than *SPEEDUP_TOLERANCE*% failures, but no more than *TOLERANCE*%, don't change output rate
* if there was more than *TOLERANCE*% failures, decrease speed by *CONVERGENCE_FACTOR*%
* if there was more than 50% of failures, enter **slow mode**

In **slow mode** requests are sent each *SLOW_DELAY* seconds:

* if all requests were successful, enter **normal mode**
* if more than 50% of requests was successful, do nothing
* if more than 50% of requests failed, enter **heartbeat mode**

In **heartbeat mode** requests are sent each *HEARTBEAT_DELAY* seconds:

* if all requests were successful, enter **slow mode**
* if there were any failures, do nothing

Each of parameters written in capital letters can be configured:

Parameter name     | Option                                          | Default value
------------------ | ----------------------------------------------- | -------------
LIMITER_PERIOD     | consumer.rate.limiterSupervisorPeriod           | 30s
SPEEDUP_TOLERANCE  | consumer.rate.failuresSpeedUpToleranceRatio     | 0.01
TOLERANCE          | consumer.rate.failuresNoChangeToleranceRatio    | 0.05
CONVERGENCE_FACTOR | consumer.rate.convergenceFactor                 | 0.2
SLOW_DELAY         | consumer.rate.limiterSlowModeDelay              | 60s
HEARTBEAT_DELAY    | consumer.rate.limiterHeartbeatModeDelay         | 60s


### How the negotiated algorithm works

By tracking latest *RATE_HISTORY_SIZE* *DELIVERY_ATTEMPT_RATE* samples,
the algorithm is able to determine busy and non-busy consumers and balance the available global rate among them over time.

- *DELIVERY_ATTEMPT_RATE* = sent_attempts / *CONSUMER_MAX_RATE*
- We consider a consumer to be busy if it's *DELIVERY_ATTEMPT_RATE* > 1 - *BUSY_TOLERANCE*
  and we try to take away a bit of rate from others.
- We don't bother do adjust more than *MIN_CHANGE_PERCENT*
  and we always preserve a *MIN_MAX_RATE* for a consumer.
- Recalculation is done every *BALANCE_INTERVAL* seconds.
- Consumer updates it's delivery attempt rate history every *UPDATE_INTERVAL*
  if the change from previous recorded value is greater than *MIN_SIGNIFICANT_CHANGE_PERCENT*.
- *MIN_SIGNIFICANT_CHANGE_PERCENT* / 100 must be lower than *BUSY_TOLERANCE*, as otherwise
  consumers would (in some cases) not enter busy state
- At the moment *RATE_HISTORY_SIZE* is ignored, defaulting to 1,
  and might be used in future versions of the algorithm



Parameter name                 | Option                                          | Default value
------------------------------ | ------------------------------------------------| --------------
BALANCE_INTERVAL               | consumer.maxrate.balanceInterval                | 30s
UPDATE_INTERVAL                | consumer.maxrate.updateInterval                 | 15s
RATE_HISTORY_SIZE              | consumer.maxrate.historySize                    | 1
BUSY_TOLERANCE                 | consumer.maxrate.busyTolerance                  | 0.1
MIN_MAX_RATE                   | consumer.maxrate.minMaxRate                     | 1.0
MIN_CHANGE_PERCENT             | consumer.maxrate.minAllowedChangePercent        | 1.0
MIN_SIGNIFICANT_CHANGE_PERCENT | consumer.maxrate.minSignificantUpdatePercent    | 9.0
