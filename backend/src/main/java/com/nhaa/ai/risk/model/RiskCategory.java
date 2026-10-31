package com.nhaa.ai.risk.model;

/**
 * Risk classification categories based on normalized SVI scores (0 - 100).
 * Prototype thresholds:
 *   0.00  – 24.99  -> LOW
 *   25.00 – 49.99  -> MODERATE
 *   50.00 – 74.99  -> HIGH
 *   75.00 – 100.00 -> CRITICAL
 *
 * NOTE: These are operational prototype thresholds, not clinical or legal determinations.
 */
public enum RiskCategory {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL
}
