package com.itexpert.content.core.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentStatsDTO {
    private String contentCode;
    private int displays;
    private int clicks;
    private Map<Integer, Long> feedbacksPerEvaluation; // clé = note (1–5), valeur = nombre
}
