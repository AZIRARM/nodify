package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.NotificationHandler;
import com.itexpert.content.lib.models.Notification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/v0/notifications")
public class NotificationEndPoint {
    private final NotificationHandler notificationHandler;

    @PostMapping(value = "/id/{notificationId}/markread")
    public Mono<Notification> markread(@PathVariable UUID notificationId, Authentication authentication) {
        return notificationHandler.markRead(authentication.getPrincipal().toString(), notificationId);
    }

    @PostMapping(value = "/markAllAsRead")
    public Flux<Notification> markAllAsRead(Authentication authentication) {
        return notificationHandler.markAllAsRead(authentication.getPrincipal().toString());
    }
}
