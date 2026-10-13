package com.hcdc.qualipea;

/**
 * Settings for the Suggested Price calculator.
 *
 * BASE_PRICE_PER_KG is a PLACEHOLDER. Replace it with a real
 * reference price for good-quality Spanish peanut kernels (e.g. from
 * your market survey / paper).
 *
 * Formula used: adjusted price per kg = base price x (1 - defect rate)
 *               estimated total       = adjusted price per kg x kilograms
 */
public final class PriceConfig {
    public static final double BASE_PRICE_PER_KG = 100.00;

    private PriceConfig() {}
}
