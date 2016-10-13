package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class MaxRateBalancer {

    Optional<Map<String, MaxRate>> balance(double subscriptionMax, Set<ConsumerRateInfo> rateInfos) {
//        double rateLeft = subscriptionMax;

        double defaultRate = subscriptionMax / Math.max(1, rateInfos.size());

//        Set<String> busyConsumers = rateInfos.stream().filter(consumerRateInfo -> {
//            RateHistory history = consumerRateInfo.getHistory();
//            history.getRates()
//        })

        // TODO: alright, here is the algorithm
        return Optional.of(rateInfos.stream()
                .collect(Collectors.toMap(ConsumerRateInfo::getConsumerId,
                        rateInfo -> new MaxRate(defaultRate))));
    }

}
