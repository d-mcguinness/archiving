package com.dmc.archiving.user.events;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when a user is deleted.
 * Other modules can listen to this event to perform cleanup operations.
 */
public class UserDeletedEvent extends ApplicationEvent {
    private final Long userId;

    public UserDeletedEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
