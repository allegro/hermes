# Consumers rate limiter

[Subscribing user guide](/user/subscribing) presents an overview of Consumers adaptive rate limiting algorithm.
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


## Maximum rate negotiation

By default, if your hermes cluster runs N consumers per subscription, the maximum global rate
for this subscription will be divided as *{subscriptionPolicy.rate} / N*
for each consumer (*ALGORITHM*=**strict**)

An incubating feature has been added in version **0.10.6**, which allows consumers to negotiate
rate among consumer instances for each subscription. Use *ALGORITHM*=**negotiated** to enable it.

### How it works

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
ALGORITHM                      | consumer.maxrate.strategy                       | strict
BALANCE_INTERVAL               | consumer.maxrate.balance.interval.seconds       | 30
UPDATE_INTERVAL                | consumer.maxrate.update.interval.seconds        | 15
RATE_HISTORY_SIZE              | consumer.maxrate.history.size                   | 1
BUSY_TOLERANCE                 | consumer.maxrate.busy.tolerance                 | 0.1
MIN_MAX_RATE                   | consumer.maxrate.min.value                      | 1.0
MIN_CHANGE_PERCENT             | consumer.maxrate.min.allowed.change.percent     | 1.0
MIN_SIGNIFICANT_CHANGE_PERCENT | consumer.maxrate.min.significant.update.percent | 9.0
