package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.RedisHandler;
import com.itexpert.content.core.models.LockInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/v0/locks")
public class RedisLockController {
    private final RedisHandler redisHandler;
    private final Duration defaultTtl = Duration.ofMinutes(5);

    public RedisLockController(RedisHandler redisHandler) {
        this.redisHandler = redisHandler;
    }

    @PostMapping("/acquire/{code}")
    public Mono<Boolean> acquire(@PathVariable String code, Authentication authentication) {
        return redisHandler.acquireLock(code, authentication.getPrincipal().toString(), defaultTtl);
    }

    @PostMapping("/release/{code}")
    public Mono<Boolean> release(@PathVariable String code, Authentication authentication) {
        return redisHandler.releaseLock(code, authentication.getPrincipal().toString());
    }

    @PostMapping("/refresh/{code}")
    public Mono<Boolean> refresh(@PathVariable String code, Authentication authentication) {
        return redisHandler.refreshLock(code, authentication.getPrincipal().toString(), defaultTtl);
    }

    @GetMapping("/owner/{code}")
    public Mono<LockInfo> getOwner(@PathVariable String code, Authentication authentication) {
        return redisHandler.getLockInfo(code, authentication);
    }

    @PostMapping("/admin/release/{code}")
    public Mono<Boolean> adminRelease(@PathVariable String code, Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            return redisHandler.forceReleaseLock(code);
        } else {
            return Mono.error(new RuntimeException("Access denied"));
        }
    }

    @GetMapping("/all")
    public Flux<LockInfo> getAllLocks(Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            return redisHandler.getAllLocks();
        } else {
            return Flux.error(new RuntimeException("Access denied"));
        }
    }
}
