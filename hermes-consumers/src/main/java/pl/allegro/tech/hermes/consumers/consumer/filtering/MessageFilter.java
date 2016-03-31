package pl.allegro.tech.hermes.consumers.consumer.filtering;

import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.function.Predicate;

interface MessageFilter extends Predicate<Message> {

    String type();

}
