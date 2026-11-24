package com.itexpert.content.core.handlers;

import com.itexpert.content.core.entities.ResourceLock;
import com.itexpert.content.core.models.LockInfo;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class LockHandler {

    private final ReactiveMongoTemplate mongoTemplate;

    private Criteria activeLockCriteria(String resourceCode, Instant now) {
        return Criteria.where("resourceCode").is(resourceCode)
                .and("expiresAt").gt(now);
    }

    // --- Acquérir un lock ---
    public Mono<Boolean> acquireLock(String resourceCode, String userId, Duration ttl) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        // Si un lock existe mais est déjà expiré, on le supprime avant insertion
        Query expiredQuery = Query.query(
                Criteria.where("resourceCode").is(resourceCode)
                        .and("expiresAt").lte(now)
        );

        ResourceLock newLock = ResourceLock.builder()
                .resourceCode(resourceCode)
                .ownerId(userId)
                .expiresAt(expiresAt)
                .build();

        return mongoTemplate.remove(expiredQuery, ResourceLock.class)
                .then(mongoTemplate.insert(newLock))
                .map(saved -> true)
                .onErrorResume(DuplicateKeyException.class, e -> Mono.just(false))
                .onErrorResume(org.springframework.dao.DuplicateKeyException.class, e -> Mono.just(false))
                .defaultIfEmpty(false);
    }

    // --- Relâcher un lock ---
    public Mono<Boolean> releaseLock(String resourceCode, String userId) {
        Query query = Query.query(
                Criteria.where("resourceCode").is(resourceCode)
                        .and("ownerId").is(userId)
        );
        return mongoTemplate.remove(query, ResourceLock.class)
                .map(result -> result.getDeletedCount() > 0)
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> refreshLock(String resourceCode, String userId, Duration ttl) {
        Instant now = Instant.now();
        Query query = Query.query(
                Criteria.where("resourceCode").is(resourceCode)
                        .and("ownerId").is(userId)
                        .and("expiresAt").gt(now)
        );
        Update update = new Update().set("expiresAt", now.plus(ttl));

        return mongoTemplate.updateFirst(query, update, ResourceLock.class)
                .map(result -> result.getMatchedCount() > 0)
                .defaultIfEmpty(false);
    }

    public Mono<LockInfo> getLockInfo(String resourceCode, String currentUser) {
        Query query = Query.query(activeLockCriteria(resourceCode, Instant.now()));

        return mongoTemplate.findOne(query, ResourceLock.class)
                .map(lock -> {
                    boolean isOwner = lock.getOwnerId() != null && lock.getOwnerId().equals(currentUser);
                    boolean locked = !isOwner;
                    return new LockInfo(lock.getOwnerId(), isOwner, locked, resourceCode);
                })
                .defaultIfEmpty(new LockInfo(null, false, false, resourceCode));
    }

    public Mono<Boolean> forceReleaseLock(String resourceCode) {
        Query query = Query.query(Criteria.where("resourceCode").is(resourceCode));
        return mongoTemplate.remove(query, ResourceLock.class)
                .map(result -> result.getDeletedCount() > 0)
                .defaultIfEmpty(false);
    }

    public Flux<LockInfo> getAllLocks() {
        Query query = Query.query(Criteria.where("expiresAt").gt(Instant.now()));
        return mongoTemplate.find(query, ResourceLock.class)
                .map(lock -> new LockInfo(lock.getOwnerId(), false, true, lock.getResourceCode()));
    }

    public Mono<Boolean> canModify(String resourceCode, String userId, Duration ttl) {
        Query query = Query.query(activeLockCriteria(resourceCode, Instant.now()));

        return mongoTemplate.findOne(query, ResourceLock.class)
                .flatMap(lock -> Mono.just(userId != null && userId.equals(lock.getOwnerId())))
                .switchIfEmpty(acquireLock(resourceCode, userId, ttl));
    }
}