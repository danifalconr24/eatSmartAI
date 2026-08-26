package com.eatsmart.domain.model;

/**
 * Result of analyzing a single supermarket product photo.
 *
 * @param product     normalized product name in Spanish
 * @param score       healthiness score from 0 to 10
 * @param nutrition   nutritional information in markdown
 * @param alternative healthier similar product, only present when score is low
 */
public record ProductAnalyzeResponse(String product, int score, String nutrition, Alternative alternative) {

    public record Alternative(String name, String reason) {
    }
}
