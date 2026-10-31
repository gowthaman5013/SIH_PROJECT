package com.nhaa.ai.risk.config;

/**
 * Centralized configuration constants and thresholds for the Safety Layer.
 * 
 * Defines operational trigger thresholds for safety-critical indicators,
 * escalation rules, and priority assignments.
 */
public final class SafetyConfiguration {

    private SafetyConfiguration() {}

    // Critical Safety Triggers (Direct escalation to CRITICAL)
    public static final double CRITICAL_IMMEDIATE_DANGER_THRESHOLD = 0.60;
    public static final double CRITICAL_SELF_HARM_THRESHOLD = 0.50;

    // Severe Violence / Threat Triggers (Tier escalation)
    public static final double SEVERE_VIOLENCE_THRESHOLD = 0.70;
    public static final double SEVERE_SEXUAL_VIOLENCE_THRESHOLD = 0.60;
    public static final double SEVERE_THREAT_THRESHOLD = 0.75;

    // Distress Indicators (Supporting context - never triggers CRITICAL on its own)
    public static final double ELEVATED_DISTRESS_THRESHOLD = 0.65;
    public static final double MODERATE_DISTRESS_THRESHOLD = 0.40;

    // Context Safety Thresholds
    public static final double HIGH_CONTEXT_RISK_THRESHOLD = 0.60;
}
