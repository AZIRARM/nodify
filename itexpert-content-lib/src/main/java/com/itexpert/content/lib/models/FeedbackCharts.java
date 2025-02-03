package com.itexpert.content.lib.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackCharts implements Serializable, Cloneable {
    private String contentCode;
    private List<Chart> charts;
    private List<Chart> verified;
    private List<Chart> notVerified;
}
