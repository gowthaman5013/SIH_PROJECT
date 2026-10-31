package com.nhaa.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VictimContextDto {

    private String victimCategory = "UNKNOWN";
    private String casteRelated = "UNKNOWN";

    public VictimContextDto() {}

    public VictimContextDto(String victimCategory, Object casteRelated) {
        this.victimCategory = victimCategory != null ? victimCategory : "UNKNOWN";
        this.casteRelated = parseTriState(casteRelated);
    }

    public String getVictimCategory() {
        return victimCategory;
    }

    public void setVictimCategory(String victimCategory) {
        this.victimCategory = victimCategory;
    }

    public String getCasteRelated() {
        return casteRelated;
    }

    public void setCasteRelated(Object casteRelated) {
        this.casteRelated = parseTriState(casteRelated);
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
