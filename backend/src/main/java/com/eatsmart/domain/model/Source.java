package com.eatsmart.domain.model;

/**
 * Citation for a health/nutrition claim.
 *
 * @param title short source name shown in the UI
 * @param url   HTTPS link to the source
 */
public record Source(String title, String url) {
}
