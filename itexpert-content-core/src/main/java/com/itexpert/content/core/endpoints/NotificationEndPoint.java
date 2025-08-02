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

    @GetMapping("/")
    public Flux<Notification> findAll() {
        return notificationHandler.findAll();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Notification>> findById(@PathVariable String id) {
        return notificationHandler.findById(UUID.fromString(id))
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
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return notificationHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/user/id/{userId}/count/unreaded")
    public Mono<ResponseEntity<Long>> countUnreadedByUserId(@PathVariable String userId) {
        return notificationHandler.countUnreaded(userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @GetMapping(value = "/user/id/{userId}/unreaded")
    public Flux<Notification> unreadedByUserId(@PathVariable String userId,
                                               @RequestParam(name = "currentPage", required = true) Integer currentPage,
                                               @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {
        return notificationHandler.unreadedByUserId(userId, currentPage, limit);
    }
    @GetMapping(value = "/user/id/{userId}/readed")
    public Flux<Notification> readedByUserId(@PathVariable String userId,
                                               @RequestParam(name = "currentPage", required = true) Integer currentPage,
                                               @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {
        return notificationHandler.readedByUserId(userId, currentPage, limit);
    }

    @PostMapping(value = "/id/{notificationId}/user/id/{userId}/markread")
    public Mono<Notification> markread(@PathVariable UUID notificationId, @PathVariable String userId) {
        return notificationHandler.markread(notificationId, userId);
    }

    @PostMapping(value = "/id/{notificationId}/user/id/{userId}/markunread")
    public Mono<Notification> markunread(@PathVariable UUID notificationId, @PathVariable String userId) {
        return notificationHandler.markunread(notificationId, userId);
    }

    @PostMapping(value = "/user/id/{userId}/markAllAsRead")
    public Flux<Notification> markAllAsRead(@PathVariable String userId) {
        return notificationHandler.markAllAsRead(userId);
    }

    @GetMapping(value = "/user/id/{userId}/countUnreaded")
    public Mono<Long> countUnreaded(@PathVariable String userId) {
        return notificationHandler.countUnreaded(userId);
    }

    @GetMapping(value = "/user/id/{userId}/countReaded")
    public Mono<Long> countReaded(@PathVariable String userId) {
        return notificationHandler.countReaded(userId);
    }
}
