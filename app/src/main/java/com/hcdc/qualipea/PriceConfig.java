
package com.hcdc.qualipea;

/**
 * PSA farmgate price reference for QualiPea.
 *
 * Source: PSA OpenSTAT
 * Dataset: Beans and Legumes: Farmgate Prices
 * Location: Region XI (Davao Region)
 * Commodity: Peanut, without shell, dry
 * Reference period: June 2026
 * Price: PHP 100.00 per kilogram
 *
 * This is a fixed reference value, not a live API request.
 * Update the constants when a newer valid PSA observation
 * becomes available and has been verified.
 */
public final class PriceConfig {

    public static final double BASE_PRICE_PER_KG = 100.00;

    public static final String SOURCE_NAME = "PSA OpenSTAT";

    public static final String LOCATION =
            "Region XI (Davao Region)";

    public static final String COMMODITY =
            "Peanut, without shell, dry";

    public static final String REFERENCE_PERIOD =
            "June 2026 farmgate price reference";

    public static final String UNIT = "PHP/kg";

    private PriceConfig() {
        // Prevent object creation.
    }
}
