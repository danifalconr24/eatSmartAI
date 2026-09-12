package com.eatsmart.application;

import java.util.Set;

final class SourceUrlPolicy {

    private static final Set<String> VERIFIED_URLS = Set.of(
            "https://www.who.int/es/health-topics/nutrition",
            "https://www.aesan.gob.es/",
            "https://www.niddk.nih.gov/health-information/informacion-de-la-salud",
            "https://www.hsph.harvard.edu/nutritionsource/");

    private SourceUrlPolicy() {
    }

    static boolean isAllowed(String url) {
        return VERIFIED_URLS.contains(url);
    }
}
