package com.nhaa.ai.risk.model;

/**
 * Operational triage priority levels determined by the Safety Layer.
 * 
 * - ROUTINE: Low risk, general informational follow-up.
 * - STANDARD: Moderate risk, standard scheduled support.
 * - PRIORITY: High risk or escalated case, expedited review and dedicated support.
 * - IMMEDIATE: Critical safety concern, emergency/crisis response, immediate human intervention.
 */
public enum SafetyPriority {
    ROUTINE,
    STANDARD,
    PRIORITY,
    IMMEDIATE
}
