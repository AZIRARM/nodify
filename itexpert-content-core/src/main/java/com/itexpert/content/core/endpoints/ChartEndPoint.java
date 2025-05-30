package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.ChartHandler;
import com.itexpert.content.core.models.TreeNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/v0/charts")
public class ChartEndPoint {

    private final ChartHandler chartHandler;

    @GetMapping("/")
    public Mono<TreeNode> getCharts() {
        return this.chartHandler.getContentStats();
    }
}
