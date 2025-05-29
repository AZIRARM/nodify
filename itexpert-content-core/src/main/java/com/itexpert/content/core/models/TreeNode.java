package com.itexpert.content.core.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TreeNode {
    private String name;
    private String code;
    private String type;
    private boolean isLeaf = false;
    private String value;
    private List<TreeNode> children;
}
