package com.eatsmart.domain.model;

import java.util.List;

public record AnalyzeResponse(List<String> products, String suggestions, int score) {
}
