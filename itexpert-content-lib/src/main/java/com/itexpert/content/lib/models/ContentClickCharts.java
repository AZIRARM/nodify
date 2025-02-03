package com.itexpert.content.lib.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ContentClickCharts implements Serializable, Cloneable {
    private String name;
    private String value;
}
