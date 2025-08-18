package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.ChartHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.models.TreeNode;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.lib.models.UserPost;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;


@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/v0/charts")
public class ChartEndPoint {

    private final ChartHandler chartHandler;

    private final UserHandler userHandler;

    @GetMapping("/")
    public Mono<TreeNode> getCharts(Authentication authentication) {
        var grantedAuthority = authentication.getAuthorities().stream().findFirst().get();

        if (grantedAuthority.getAuthority().equals(RoleEnum.ADMIN.name())) {

            return chartHandler.getContentStats(List.of());
        }
        return this.userHandler.findByEmail(authentication.getPrincipal().toString())
                .map(UserPost::getProjects)
                .flatMap(chartHandler::getContentStats);
    }
}
