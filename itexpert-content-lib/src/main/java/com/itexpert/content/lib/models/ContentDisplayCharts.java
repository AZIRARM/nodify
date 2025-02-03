package com.itexpert.content.lib.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ContentDisplayCharts implements Serializable, Cloneable {
    private String name;
    private String value;
}
