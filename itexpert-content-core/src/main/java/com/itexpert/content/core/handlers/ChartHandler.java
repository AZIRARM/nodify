package com.itexpert.content.core.handlers;

import com.itexpert.content.core.models.TreeNode;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Chart;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class ChartHandler {

    private final ContentDisplayHandler contentDisplayHandler;
    private final NodeHandler nodeHandler;
    private final ContentNodeHandler contentNodeHandler;

    public Mono<TreeNode> getContentStats(List<String> userProjects) {
        return this.nodeHandler.findParentsNodesByStatus(StatusEnum.PUBLISHED.name())
                .filter(children -> userProjects.isEmpty() || userProjects.contains(children.getCode()) || userProjects.contains(children.getParentCode()) || userProjects.contains(children.getParentCodeOrigin()))
                .flatMap(this::buildTreeFromNode)
                .collectList()
                .map(trees -> {
                    trees.sort(Comparator.comparing(TreeNode::getName, Comparator.nullsLast(String::compareToIgnoreCase)));

                    TreeNode root = new TreeNode();
                    root.setName("Nodify");
                    root.setCode("Nodify");
                    root.setType("NODIFY");
                    root.setChildren(trees);
                    return root;
                });
    }

    private Mono<TreeNode> buildTreeFromNode(Node node) {
        TreeNode treeNode = new TreeNode();
        treeNode.setName(node.getName());
        treeNode.setCode(node.getCode());
        treeNode.setType("NODE");

        List<TreeNode> children = new ArrayList<>();

        if (node.getContents() != null) {
            for (ContentNode content : node.getContents()) {
                TreeNode leaf = new TreeNode();
                leaf.setName(ObjectUtils.isEmpty(content.getDescription()) ? content.getCode() : content.getDescription());
                leaf.setCode(content.getCode());
                leaf.setChildren(Collections.emptyList());
                leaf.setLeaf(Boolean.TRUE);
                leaf.setType(content.getType().name());
                children.add(leaf);
            }
        }


        return this.nodeHandler.findAllByParentCodeAndStatus(node.getCode(), StatusEnum.PUBLISHED.name())
                .flatMap(parent -> setContentsNodeWithStatus(parent, StatusEnum.PUBLISHED.name()))
                .flatMap(this::buildTreeFromNode)
                .collectList()
                .map(subTrees -> {
                    children.addAll(subTrees);
                    treeNode.setChildren(children);
                    return treeNode;
                })
                .flatMap(this::fillDisplays);
    }

    private Mono<Node> setContentsNodeWithStatus(Node node, String status) {
        return contentNodeHandler.findAllByNodeCodeAndStatus(node.getCode(), status)
                .collectList()
                .map(contents -> {
                    node.setContents(contents);
                    return node;
                });
    }

    private Mono<TreeNode> fillDisplays(TreeNode treeParent) {
        List<TreeNode> children = treeParent.getChildren();

        return Flux.fromIterable(children)
                .flatMap(child -> {
                    if (child.isLeaf()) {
                        return this.contentDisplayHandler.getChartsByContentCode(child.getCode())
                                .collectList()
                                .map(displayCharts -> {
                                    if (ObjectUtils.isEmpty(child.getChildren())) {
                                        child.setChildren(new ArrayList<>());
                                    }

                                    // Supprimer un nœud DISPLAYED existant
                                    child.getChildren().removeIf(n -> "DISPLAYED".equals(n.getName()));

                                    TreeNode displayNode = new TreeNode();
                                    displayNode.setName("DISPLAYED");
                                    displayNode.setType("DISPLAYED");

                                    // Calcul de la somme des valeurs
                                    long total = displayCharts.stream()
                                            .mapToLong(chart -> {
                                                try {
                                                    return Long.parseLong(chart.getValue());
                                                } catch (NumberFormatException e) {
                                                    return 0;
                                                }
                                            })
                                            .sum();

                                    displayNode.setValue(String.valueOf(total));
                                    displayNode.setLeaf(true);

                                    child.getChildren().add(displayNode);
                                    return child;
                                });
                    } else {
                        return fillDisplays(child);
                    }
                })
                .collectList()
                .map(updatedChildren -> {
                    treeParent.setChildren(updatedChildren);
                    return treeParent;
                });
    }

    private List<Chart> aggregateChartsByName(List<Chart> charts) {
        Map<String, Integer> aggregated = new HashMap<>();

        for (Chart chart : charts) {
            int numericValue = 0;
            try {
                numericValue = Integer.parseInt(chart.getValue());
            } catch (NumberFormatException e) {
                // Ignore or log invalid value
            }

            aggregated.merge(chart.getName(), numericValue, Integer::sum);
        }

        // Construire la nouvelle liste de Chart avec les sommes
        return aggregated.entrySet().stream()
                .map(entry -> new Chart(entry.getKey(), String.valueOf(entry.getValue()), false))
                .collect(Collectors.toList());
    }

}
