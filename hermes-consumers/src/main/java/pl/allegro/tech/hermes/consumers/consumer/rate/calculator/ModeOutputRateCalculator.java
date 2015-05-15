package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.consumers.consumer.rate.DeliveryCounters;

interface ModeOutputRateCalculator {

    OutputRateCalculationResult calculateOutputRate(double currentRate, double maximumOutputRate, DeliveryCounters counters);

}
