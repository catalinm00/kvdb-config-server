package com.kvdbcs.infrastructure.listener;

import com.kvdbcs.application.service.NotifyInstancesUpdatedService;
import com.kvdbcs.domain.event.UpdatedInstanceEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;

@Singleton
public class UpdatedInstanceEventListener {
    private final NotifyInstancesUpdatedService notifyInstancesUpdatedService;

    public UpdatedInstanceEventListener(NotifyInstancesUpdatedService notifyInstancesUpdatedService) {
        this.notifyInstancesUpdatedService = notifyInstancesUpdatedService;
    }

    @EventListener
    public void listen(UpdatedInstanceEvent event) {
        notifyInstancesUpdatedService.Execute();
    }
}
