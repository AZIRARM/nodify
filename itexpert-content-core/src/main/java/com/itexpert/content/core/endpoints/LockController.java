package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.LockHandler;
import com.itexpert.content.core.models.LockInfo;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/v0/locks")
public class LockController {
    private final LockHandler lockHandler;
    private final Duration defaultTtl = Duration.ofMinutes(5);

    public LockController(LockHandler lockHandler) {
        this.lockHandler = lockHandler;
    }

    @PostMapping("/acquire/{code}")
    public Mono<Boolean> acquire(@PathVariable String code, Authentication authentication) {
        return lockHandler.acquireLock(code, authentication.getPrincipal().toString(), defaultTtl);
    }

    @PostMapping("/release/{code}")
    public Mono<Boolean> release(@PathVariable String code, Authentication authentication) {
        return lockHandler.releaseLock(code, authentication.getPrincipal().toString());
    }

    @PostMapping("/refresh/{code}")
    public Mono<Boolean> refresh(@PathVariable String code, Authentication authentication) {
        return lockHandler.refreshLock(code, authentication.getPrincipal().toString(), defaultTtl);
    }

    @GetMapping("/owner/{code}")
    public Mono<LockInfo> getOwner(@PathVariable String code, Authentication authentication) {
        return lockHandler.getLockInfo(code, authentication.getPrincipal().toString());
    }

    @PostMapping("/admin/release/{code}")
    public Mono<Boolean> adminRelease(@PathVariable String code, Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            return lockHandler.forceReleaseLock(code);
        } else {
            return Mono.error(new RuntimeException("Access denied"));
        }
    }

    @GetMapping("/all")
    public Flux<LockInfo> getAllLocks(Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
            return lockHandler.getAllLocks();
        } else {
            return Flux.error(new RuntimeException("Access denied"));
        }
    }
}