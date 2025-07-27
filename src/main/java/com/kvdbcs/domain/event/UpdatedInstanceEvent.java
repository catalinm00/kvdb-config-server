package com.kvdbcs.domain.event;

import io.micronaut.context.event.ApplicationEvent;

public class UpdatedInstanceEvent extends ApplicationEvent {
    public UpdatedInstanceEvent(Object source) {
        super(source);
    }
}
