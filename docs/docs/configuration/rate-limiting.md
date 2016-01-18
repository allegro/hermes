# Consumers rate limiter

[Subscribing user guide](/user/subscribing) presents an overview of Consumers adaptive rate limiting algorithm.
This section describes the algorithm in detail along with any describing all configuration options that can be used
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

In **heatbeat mode** requests are sent each *HEARTBEAT_DELAY* seconds:

* if all requests were successful, enter **slow mode**
* if there were any failures, do nothing

Each of parameters written in capital letters can be configured:

Parameter name     | Option                                          | Default value
------------------ | ----------------------------------------------- | -------------
LIMITER_PERIOD     | consumer.rate.limiter.supervisor.period         | 30
SPEEDUP_TOLERANCE  | consumer.rate.failures.speedup.tolerance.ratio  | 0.01
TOLERANCE          | consumer.rate.failures.nochange.tolerance.ratio | 0.05
CONVERGENCE_FACTOR | consumer.rate.convergence.factor                | 0.2
SLOW_DELAY         | consumer.rate.limiter.slow.mode.delay           | 1
HEARTBEAT_DELAY    | consumer.rate.limiter.hearbeat.mode.delay       | 60
