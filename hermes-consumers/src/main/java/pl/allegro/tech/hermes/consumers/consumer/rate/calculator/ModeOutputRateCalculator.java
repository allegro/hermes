package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

interface ModeOutputRateCalculator {

    OutputRateCalculationResult calculateOutputRate(double currentRate,
                                                    double maximumOutputRate,
                                                    SendCounters counters);

}
