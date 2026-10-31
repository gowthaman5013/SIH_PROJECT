package com.nhaa.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SafetyContextDto {

    private String currentThreat = "UNKNOWN";
    private String ongoingThreats = "UNKNOWN";
    private String immediateSafetyConcern = "UNKNOWN";
    private String retaliationConcern = "UNKNOWN";
    private String protectionRequested = "UNKNOWN";
    private String protectionUnavailable = "UNKNOWN";
    private String witnessStatus = "UNKNOWN";
    private String perpetratorPresent = "UNKNOWN";

    public SafetyContextDto() {}

    public SafetyContextDto(String currentThreat, String immediateSafetyConcern,
                            String retaliationConcern, String protectionRequested) {
        this.currentThreat = parseTriState(currentThreat);
        this.immediateSafetyConcern = parseTriState(immediateSafetyConcern);
        this.retaliationConcern = parseTriState(retaliationConcern);
        this.protectionRequested = parseTriState(protectionRequested);
    }

    public String getCurrentThreat() {
        return currentThreat;
    }

    public void setCurrentThreat(Object currentThreat) {
        this.currentThreat = parseTriState(currentThreat);
    }

    public String getOngoingThreats() {
        return ongoingThreats;
    }

    public void setOngoingThreats(Object val) {
        this.ongoingThreats = parseTriState(val);
    }

    public String getImmediateSafetyConcern() {
        return immediateSafetyConcern;
    }

    public void setImmediateSafetyConcern(Object val) {
        this.immediateSafetyConcern = parseTriState(val);
    }

    public String getRetaliationConcern() {
        return retaliationConcern;
    }

    public void setRetaliationConcern(Object val) {
        this.retaliationConcern = parseTriState(val);
    }

    public String getProtectionRequested() {
        return protectionRequested;
    }

    public void setProtectionRequested(Object val) {
        this.protectionRequested = parseTriState(val);
    }

    public String getProtectionUnavailable() {
        return protectionUnavailable;
    }

    public void setProtectionUnavailable(Object val) {
        this.protectionUnavailable = parseTriState(val);
    }

    public String getWitnessStatus() {
        return witnessStatus;
    }

    public void setWitnessStatus(Object val) {
        this.witnessStatus = parseTriState(val);
    }

    public String getPerpetratorPresent() {
        return perpetratorPresent;
    }

    public void setPerpetratorPresent(Object val) {
        this.perpetratorPresent = parseTriState(val);
    }

    private static String parseTriState(Object val) {
        if (val instanceof Boolean b) {
            return b ? "TRUE" : "FALSE";
        } else if (val != null) {
            String s = val.toString().trim().toUpperCase();
            if (s.equals("TRUE") || s.equals("FALSE")) return s;
        }
        return "UNKNOWN";
    }
}
