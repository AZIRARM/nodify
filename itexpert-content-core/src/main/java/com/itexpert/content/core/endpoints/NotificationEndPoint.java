package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.NotificationHandler;
import com.itexpert.content.lib.models.Notification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/user/{userId}")
    public Flux<Notification> findAll(@PathVariable UUID userId) {
        return notificationHandler.findAll(userId);
    }

    @GetMapping(value = "/user/{userId}/id/{id}")
    public Mono<ResponseEntity<Notification>> findById(@PathVariable UUID userId, @PathVariable UUID id) {
        return notificationHandler.findById(userId, id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    // @RolesAllowed("ADMIN")
    @PostMapping("/")
    public Mono<ResponseEntity<Notification>> save(@RequestBody(required = true) Notification notification) {
        return notificationHandler.save(notification)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable UUID id) {
        return notificationHandler.delete(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/user/id/{userId}/count/unreaded")
    public Mono<ResponseEntity<Long>> countUnreadedByUserId(@PathVariable UUID userId) {
        return notificationHandler.countUnreaded(userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @GetMapping(value = "/user/id/{userId}/unreaded")
    public Flux<Notification> unreadedByUserId(@PathVariable UUID userId,
                                               @RequestParam(name = "currentPage", required = true) Integer currentPage,
                                               @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {
        return notificationHandler.unreadedByUserId(userId, currentPage, limit);
    }
    @GetMapping(value = "/user/id/{userId}/readed")
    public Flux<Notification> readedByUserId(@PathVariable UUID userId,
                                               @RequestParam(name = "currentPage", required = true) Integer currentPage,
                                               @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {
        return notificationHandler.readedByUserId(userId, currentPage, limit);
    }

    @PostMapping(value = "/id/{notificationId}/user/id/{userId}/markread")
    public Mono<Notification> markread(@PathVariable UUID notificationId, @PathVariable UUID userId) {
        return notificationHandler.markRead(userId, notificationId );
    }

    @PostMapping(value = "/id/{notificationId}/user/id/{userId}/markunread")
    public Mono<Notification> markunread(@PathVariable UUID notificationId, @PathVariable UUID userId) {
        return notificationHandler.markUnread(userId, notificationId);
    }

    @PostMapping(value = "/user/id/{userId}/markAllAsRead")
    public Flux<Notification> markAllAsRead(@PathVariable UUID userId) {
        return notificationHandler.markAllAsRead(userId);
    }

    @GetMapping(value = "/user/id/{userId}/countUnreaded")
    public Mono<Long> countUnreaded(@PathVariable UUID userId) {
        return notificationHandler.countUnreaded(userId);
    }

    @GetMapping(value = "/user/id/{userId}/countReaded")
    public Mono<Long> countReaded(@PathVariable UUID userId) {
        return notificationHandler.countReaded(userId);
    }
}
