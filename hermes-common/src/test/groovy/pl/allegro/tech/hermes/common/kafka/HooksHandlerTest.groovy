package pl.allegro.tech.hermes.common.kafka

import org.glassfish.hk2.api.ServiceLocator
import pl.allegro.tech.hermes.common.hook.Hook
import pl.allegro.tech.hermes.common.hook.HooksHandler
import pl.allegro.tech.hermes.common.hook.ServiceAwareHook
import spock.lang.Specification


class HooksHandlerTest extends Specification {

    HooksHandler hooksHandler = new HooksHandler();

    def "should execute hooks in correct order"() {
        given:
        List<Integer> hooksExecutionOrder = new ArrayList<>()
        addHook(hooksExecutionOrder, 0, Hook.NORMAL_PRIORITY)
        addHook(hooksExecutionOrder, 1, Hook.NORMAL_PRIORITY)
        addHook(hooksExecutionOrder, 2, Hook.LOWER_PRIORITY)
        addHook(hooksExecutionOrder, 3, Hook.LOWER_PRIORITY)
        addHook(hooksExecutionOrder, 4, Hook.HIGHER_PRIORITY)
        addHook(hooksExecutionOrder, 5, Hook.HIGHER_PRIORITY)

        when:
        hooksHandler.shutdown(null)

        then:
        hooksExecutionOrder.equals([4, 5, 0, 1, 2, 3])
    }

    private int addHook(List<Integer> hooksExecutionOrder, int order, int priority) {
        hooksHandler.addShutdownHook(new ServiceAwareHook() {
            @Override
            void accept(ServiceLocator serviceLocator) {
                hooksExecutionOrder.add(order)
            }

            @Override
            int getPriority() {
                return priority
            }
        })

        return order
    }
}
