package com.itexpert.content.core.utils.auth;


import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.models.Node;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component("NodeResponseChecker")
@AllArgsConstructor
public class NodeResponseChecker {

    private final UserHandler userHandler;

    public Flux<Node> filter(Flux<Node> nodes) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> filterFactory(securityContext, nodes)).block();
    }

    public Flux<Node> filterFactory(SecurityContext securityContext, Flux<Node> nodes) {
        Mono<Flux<Node>> fluxMono = null;
        return (Flux) this.userHandler.findByEmail(securityContext.getAuthentication().getName())
                .subscribe(userPost -> nodes.filter(node -> userPost.getProjects().contains(node.getCode())));

    }

}
