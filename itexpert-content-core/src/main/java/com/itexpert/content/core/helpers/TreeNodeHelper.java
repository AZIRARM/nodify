package com.itexpert.content.core.helpers;

import com.itexpert.content.core.models.TreeNode;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class TreeNodeHelper {
    public Mono<TreeNode> buildTreeWithContent(List<Node> flatNodes, Node origin) {
        Map<String, TreeNode> nodeMap = new HashMap<>();
        Map<String, Node> rawMap = flatNodes.stream()
                .collect(Collectors.toMap(Node::getCode, Function.identity()));

        // Créer les TreeNode de chaque Node (sans contenu pour l'instant)
        for (Node n : flatNodes) {
            TreeNode node = new TreeNode(n.getName());
            // Ajouter les contenus comme enfants directs
            for (ContentNode c : n.getContents()) {
                node.addChild(new TreeNode(c.getCode()));
            }
            nodeMap.put(n.getCode(), node);
        }

        TreeNode root = new TreeNode(origin.getName());

        // Rattacher les enfants aux parents
        for (Node n : flatNodes) {
            TreeNode current = nodeMap.get(n.getCode());

            if (n.getParentCode() == null) {
                root.addChild(current);
            } else {
                TreeNode parent = nodeMap.get(n.getParentCode());
                if (parent != null) {
                    parent.addChild(current);
                } else {
                    root.addChild(current); // Parent manquant : rattacher à la racine
                }
            }
        }

        return Mono.just(root);
    }


}
