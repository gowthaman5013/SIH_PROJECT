package com.nhaa.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncidentContextDto {

    private String incidentType = "UNKNOWN";
    private List<String> incidentSubTypes = new ArrayList<>();
    private String incidentDescription = "";
    private String currentStatus = "UNKNOWN";
    private String repeatedIncident = "UNKNOWN";
    private String escalationPresent = "UNKNOWN";
    private String physicalViolence = "UNKNOWN";
    private String weaponUsage = "UNKNOWN";
    private String weaponOrObject = "";
    private String casteRelatedMotive = "UNKNOWN";
    private String areaRestriction = "UNKNOWN";
    private String publicView = "UNKNOWN";

    public IncidentContextDto() {}

    public IncidentContextDto(String incidentType, List<String> incidentSubTypes, String incidentDescription,
                              String currentStatus, String repeatedIncident, String escalationPresent) {
        this.incidentType = incidentType;
        this.incidentSubTypes = incidentSubTypes != null ? incidentSubTypes : new ArrayList<>();
        this.incidentDescription = incidentDescription;
        this.currentStatus = currentStatus;
        this.repeatedIncident = repeatedIncident != null ? repeatedIncident : "UNKNOWN";
        this.escalationPresent = escalationPresent != null ? escalationPresent : "UNKNOWN";
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public List<String> getIncidentSubTypes() {
        return incidentSubTypes;
    }

    public void setIncidentSubTypes(List<String> incidentSubTypes) {
        this.incidentSubTypes = incidentSubTypes != null ? incidentSubTypes : new ArrayList<>();
    }

    public String getIncidentDescription() {
        return incidentDescription;
    }

    public void setIncidentDescription(String incidentDescription) {
        this.incidentDescription = incidentDescription;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getRepeatedIncident() {
        return repeatedIncident;
    }

    public void setRepeatedIncident(Object repeatedIncident) {
        if (repeatedIncident instanceof Boolean b) {
            this.repeatedIncident = b ? "TRUE" : "FALSE";
        } else if (repeatedIncident != null) {
            this.repeatedIncident = repeatedIncident.toString().toUpperCase();
        } else {
            this.repeatedIncident = "UNKNOWN";
        }
    }

    public String getEscalationPresent() {
        return escalationPresent;
    }

    public void setEscalationPresent(Object escalationPresent) {
        if (escalationPresent instanceof Boolean b) {
            this.escalationPresent = b ? "TRUE" : "FALSE";
        } else if (escalationPresent != null) {
            this.escalationPresent = escalationPresent.toString().toUpperCase();
        } else {
            this.escalationPresent = "UNKNOWN";
        }
    }

    public String getPhysicalViolence() {
        return physicalViolence;
    }

    public void setPhysicalViolence(Object val) {
        this.physicalViolence = parseTriState(val);
    }

    public String getWeaponUsage() {
        return weaponUsage;
    }

    public void setWeaponUsage(Object val) {
        this.weaponUsage = parseTriState(val);
    }

    public String getWeaponOrObject() {
        return weaponOrObject;
    }

    public void setWeaponOrObject(String weaponOrObject) {
        this.weaponOrObject = weaponOrObject != null ? weaponOrObject.trim() : "";
    }

    public String getCasteRelatedMotive() {
        return casteRelatedMotive;
    }

    public void setCasteRelatedMotive(Object val) {
        this.casteRelatedMotive = parseTriState(val);
    }

    public String getAreaRestriction() {
        return areaRestriction;
    }

    public void setAreaRestriction(Object val) {
        this.areaRestriction = parseTriState(val);
    }

    public String getPublicView() {
        return publicView;
    }

    public void setPublicView(Object val) {
        this.publicView = parseTriState(val);
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
