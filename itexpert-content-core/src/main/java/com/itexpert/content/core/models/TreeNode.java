package com.itexpert.content.core.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TreeNode {
    private String name;
    private List<TreeNode> children = new ArrayList<>();

    public TreeNode(String name) {
        this.name = name;
    }

    public void addChild(TreeNode child) {
        this.children.add(child);
    }

}
